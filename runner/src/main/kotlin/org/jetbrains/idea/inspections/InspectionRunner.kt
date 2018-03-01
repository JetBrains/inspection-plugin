package org.jetbrains.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.ApplicationInspectionProfileManager
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.createCommandLineApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.PlatformUtils
import org.gradle.api.GradleException
import org.gradle.api.Project as GradleProject
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.jdom2.Document as JdomDocument
import org.jetbrains.intellij.*
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import com.intellij.openapi.editor.Document as IdeaDocument

@Suppress("unused")
class InspectionRunner(
        private val project: GradleProject,
        private val maxErrors: Int,
        private val maxWarnings: Int,
        private val quiet: Boolean,
        private val inspectionClasses: InspectionClassesSuite,
        private val reports: IdeaCheckstyleReports,
        private val logger: Logger
) : Analyzer {
    companion object {
        private const val AWT_HEADLESS = "java.awt.headless"
        private const val IDEA_HOME_PATH = "idea.home.path"
        private const val BUILD_NUMBER = "idea.plugins.compatible.build"
        private const val SYSTEM_PATH = "idea.system.path"
        private const val USER_HOME = "user.home"
        private const val INSPECTION_PROFILES_PATH = ".idea/inspectionProfiles"
        private const val SYSTEM_MARKER_FILE = "marker.ipl"

        private val USELESS_PLUGINS = listOf(
                "mobi.hsz.idea.gitignore",
                "org.jetbrains.plugins.github",
                "Git4Idea"
        )
    }

    private val projectPath: String = project.rootProject.projectDir.absolutePath

    // Returns true if analysis executed successfully
    override fun analyzeTreeAndLogResults(tree: FileTree): Boolean {
        logger.info("Class loader: " + this.javaClass.classLoader)
        logger.info("Input classes: " + inspectionClasses)
        val (application, systemPathMarkerChannel) = loadApplication()
        try {
            application.doNotSave()
            val results = application.analyzeTree(tree)
            var errors = 0
            var warnings = 0

            val generators = listOf(
                    XMLGenerator(reports.xml),
                    HTMLGenerator(reports.html, application)
            ).filter { it.enabled }

            var success = true
            logger.info("Total of ${results.values.flatten().size} problems found")
            val sortedResults = results.entries.map { entry -> entry.value.map { entry.key to it }}.flatten().sortedBy {
                val line = it.second.line
                val row = it.second.row
                (line shl 16) + row
            }.groupBy { it.second.fileName }

            analysisLoop@ for (fileInspectionAndProblems in sortedResults.values) {
                for ((inspectionClass, problem) in fileInspectionAndProblems) {
                    val level = problem.actualLevel(inspectionClasses.getLevel(inspectionClass)) ?: continue
                    when (level) {
                        ProblemLevel.ERROR -> errors++
                        ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> warnings++
                        ProblemLevel.INFORMATION -> {
                        }
                    }
                    if (!quiet) {
                        logger.log(level.logLevel, problem.renderWithLocation())
                    }
                    generators.forEach { it.report(problem, level, inspectionClass) }
                    if (errors > maxErrors) {
                        logger.error("Too many errors found: $errors. Analysis stopped")
                        success = false
                        break@analysisLoop
                    }
                    if (warnings > maxWarnings) {
                        logger.error("Too many warnings found: $warnings. Analysis stopped")
                        success = false
                        break@analysisLoop
                    }
                }
            }

            generators.forEach { it.generate() }
            return success
        } catch (e: Throwable) {
            if (e is GradleException) throw e
            throw GradleException("EXCEPTION caught in inspection plugin (IDEA runReadAction): " + e, e)
        } finally {
            systemPathMarkerChannel.close()
            // NB: exit is actually performed on EDT thread!
            application.exit(true, true)
        }
    }

    private fun generateSystemPath(buildNumber: String): Pair<String, FileChannel> {
        val homeDir = System.getProperty(USER_HOME).replace("\\", "/")
        val buildPrefix = buildNumber.replace(".", "_")
        var path: String
        var code = 0
        var channel: FileChannel
        do {
            code++
            path = "$homeDir/.IntellijIDEAInspections/${buildPrefix}_code$code/system"
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            File(file, SYSTEM_MARKER_FILE).createNewFile()
            // To prevent usages by multiple processes
            val lock = try {
                channel = FileChannel.open(Paths.get(path, SYSTEM_MARKER_FILE), StandardOpenOption.WRITE)
                channel.tryLock()
            } catch (e: IOException) {
                logger.warn("IO exception while locking: ${e.message}")
                throw GradleException("EXCEPTION caught in inspection plugin (IDEA system dir lock): " + e, e)
            }
            if (lock == null) {
                if (code == 256) {
                    throw GradleException("Cannot create IDEA system directory (all locked)")
                }
            }
        } while (lock == null)
        return path to channel
    }

    private fun loadApplication(): Pair<ApplicationEx, FileChannel> {
        System.setProperty(IDEA_HOME_PATH, UnzipTask.cacheDirectory.path)
        System.setProperty(AWT_HEADLESS, "true")
        val buildNumber = UnzipTask.buildNumber()
        System.setProperty(BUILD_NUMBER, buildNumber)
        val (systemPath, systemPathMarkerChannel) = generateSystemPath(buildNumber)
        System.setProperty(SYSTEM_PATH, systemPath)

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX))
        logger.warn("IDEA home path: " + PathManager.getHomePath())
        logger.warn("IDEA system path: $systemPath")
        createCommandLineApplication(isInternal = false, isUnitTestMode = false, isHeadless = true)
        for (plugin in USELESS_PLUGINS) {
            PluginManagerCore.disablePlugin(plugin)
        }

        logger.info("Plugins enabled: " + PluginManagerCore.getPlugins().toList())
        ApplicationManagerEx.getApplicationEx().load()
        return (ApplicationManagerEx.getApplicationEx() ?: run {
            throw GradleException("Cannot create IDEA application")
        }) to systemPathMarkerChannel
    }

    private class PluginInspectionWrapper(
            val tool: InspectionProfileEntry,
            val extension: InspectionEP?,
            val classFqName: String,
            level: String? = null
    ) {
        val level: ProblemLevel? = level?.let { ProblemLevel.fromInspectionEPLevel(it) }
    }

    private fun InspectionClassesSuite.getInspectionWrappers(
            ideaProject: IdeaProject
    ): Sequence<PluginInspectionWrapper> {
        if (inheritFromIdea) {
            val inspectionProfileManager = ApplicationInspectionProfileManager.getInstanceImpl()
            val profilePath = "$projectPath/$INSPECTION_PROFILES_PATH/$ideaProfile"
            val inspectionProfile = inspectionProfileManager.loadProfile(profilePath)
                                    ?: inspectionProfileManager.currentProfile

            val profileTools = inspectionProfile.getAllEnabledInspectionTools(ideaProject)
            return profileTools.asSequence().map {
                val wrapper = it.tool
                PluginInspectionWrapper(wrapper.tool, wrapper.extension, wrapper.tool.javaClass.name,
                        it.defaultState.level.name)
            }
        }
        return classes.asSequence().mapNotNull { inspectionClassName ->
            val tools = InspectionToolRegistrar.getInstance().createTools()
            val inspectionToolWrapper = tools.find { it.tool.javaClass.name == inspectionClassName }
            if (inspectionToolWrapper == null) {
                logger.error("Inspection $inspectionClassName is not found in registrar")
                null
            }
            else {
                inspectionToolWrapper.let {
                    PluginInspectionWrapper(it.tool, it.extension, inspectionClassName)
                }
            }
        }
    }

    private fun Application.analyzeTree(tree: FileTree): Map<String, List<PinnedProblemDescriptor>> {
        logger.info("Before project creation at '$projectPath'")
        val ideaProject: IdeaProject = run {
            var ideaProject: IdeaProject? = null
            val projectFileName = project.rootProject.name + ProjectFileType.DOT_DEFAULT_EXTENSION
            val projectFile = File(projectPath, projectFileName)
            invokeAndWait {
                ideaProject = ProjectUtil.openOrImport(projectFile.absolutePath, null, false)
            }
            ideaProject ?: run {
                throw GradleException("Cannot open IDEA project: '${projectFile.absolutePath}'")
            }
        }
        logger.info("Before psi manager creation")
        val psiManager = PsiManager.getInstance(ideaProject)
        logger.info("Before virtual file manager creation")
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFileSystem = virtualFileManager.getFileSystem("file")
        val documentManager = FileDocumentManager.getInstance()

        val results: MutableMap<String, MutableList<PinnedProblemDescriptor>> = mutableMapOf()
        logger.info("Before inspections launched: total of ${tree.files.size} files to analyze")
        inspectionLoop@ for (inspectionWrapper in inspectionClasses.getInspectionWrappers(ideaProject)) {
            val inspectionClassName = inspectionWrapper.classFqName
            val inspectionTool = inspectionWrapper.tool
            when (inspectionTool) {
                is LocalInspectionTool -> {}
                is GlobalInspectionTool -> {
                    logger.warn("Global inspection tools like $inspectionClassName are not yet supported")
                    continue@inspectionLoop
                }
                else -> {
                    logger.error("Unexpected $inspectionClassName which is neither local nor global")
                    continue@inspectionLoop
                }
            }
            val inspectionExtension = inspectionWrapper.extension
            val displayName = inspectionExtension?.displayName ?: "<Unknown diagnostic>"
            val inspectionResults = mutableListOf<PinnedProblemDescriptor>()
            runReadAction {
                for (sourceFile in tree) {
                    val filePath = sourceFile.absolutePath
                    val virtualFile = virtualFileSystem.findFileByPath(filePath)
                                      ?: throw GradleException("Cannot find virtual file for $filePath")
                    val psiFile = psiManager.findFile(virtualFile)
                                  ?: throw GradleException("Cannot find PSI file for $filePath")
                    val document = documentManager.getDocument(virtualFile)
                                   ?: throw GradleException("Cannot get document for $filePath")
                    inspectionResults += inspectionTool.analyze(psiFile, document, displayName, inspectionWrapper.level)
                }
            }
            results[inspectionClassName] = inspectionResults
        }
        return results
    }

    private fun LocalInspectionTool.analyze(
            file: PsiFile,
            document: IdeaDocument,
            displayName: String?,
            problemLevel: ProblemLevel?
    ): List<PinnedProblemDescriptor> {
        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        val visitor = this.buildVisitor(holder, false, session)
        file.acceptRecursively(visitor)
        return holder.results.map {
            PinnedProblemDescriptor(it, document, displayName, problemLevel)
        }
    }
}