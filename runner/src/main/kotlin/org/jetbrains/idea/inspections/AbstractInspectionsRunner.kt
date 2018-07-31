package org.jetbrains.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.ApplicationInspectionProfileManager
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.Application
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.impl.ProjectManagerImpl
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.intellij.*
import java.io.File


abstract class AbstractInspectionsRunner(
        private val projectPath: String,
        testMode: Boolean,
        private val inheritFromIdea: Boolean,
        private val inspections: Set<String>?,
        private val ideaProfile: String?
) : IdeaRunner(testMode) {

    companion object {
        private const val KT_LIB = "kotlin-stdlib"
        private const val INSPECTION_PROFILES_PATH = ".idea/inspectionProfiles"
        private val KOTLIN_FILE_APPLICABLE_LANGUAGES = setOf(null, "kotlin", "UAST")
        private val JAVA_FILE_APPLICABLE_LANGUAGES = setOf(null, "java", "UAST", JavaLanguage.INSTANCE.id)
    }

    class PluginInspectionWrapper(
            val tool: InspectionProfileEntry,
            val extension: InspectionEP?,
            val classFqName: String,
            val language: String?,
            level: String? = null
    ) {
        val level: ProblemLevel? = level?.let { ProblemLevel.fromInspectionEPLevel(it) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PluginInspectionWrapper

            if (classFqName != other.classFqName) return false

            return true
        }

        override fun hashCode(): Int {
            return classFqName.hashCode()
        }
    }

    private fun getInspectionWrappers(ideaProject: IdeaProject): Sequence<PluginInspectionWrapper> {
        if (inheritFromIdea) return getInspectionWrappersInheritFromIdea(ideaProject)
        return getRegisteredInspectionWrappers()
    }

    private fun getInspectionWrappersInheritFromIdea(ideaProject: IdeaProject): Sequence<PluginInspectionWrapper> {
        val inspectionProfileManager = ApplicationInspectionProfileManager.getInstanceImpl()
        val profilePath = "$projectPath/$INSPECTION_PROFILES_PATH/$ideaProfile"
        val inspectionProfile = inspectionProfileManager.loadProfile(profilePath)
                ?: inspectionProfileManager.currentProfile
        val profileTools = inspectionProfile.getAllEnabledInspectionTools(ideaProject)
        return profileTools.asSequence().map {
            val wrapper = it.tool
            PluginInspectionWrapper(
                    wrapper.tool,
                    wrapper.extension,
                    wrapper.tool.javaClass.name,
                    wrapper.language,
                    it.defaultState.level.name
            )
        }
    }

    private fun getRegisteredInspectionWrappers(): Sequence<PluginInspectionWrapper> {
        require(inspections != null) { "Runner inspections hasn't initialized" }
        val tools = InspectionToolRegistrar.getInstance().createTools()
        return inspections!!.asSequence().mapNotNull { inspectionClassName ->
            val inspectionToolWrapper = tools.find { it.tool.javaClass.name == inspectionClassName }
            if (inspectionToolWrapper == null) {
                logger.error("InspectionsTask $inspectionClassName is not found in registrar")
                null
            } else {
                inspectionToolWrapper.let {
                    PluginInspectionWrapper(it.tool, it.extension, inspectionClassName, it.language)
                }
            }
        }
    }

    override fun analyze(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            parameters: AnalyzerParameters
    ): Boolean {
        val reportParameters = parameters.reportParameters
        val quickFixParameters = parameters.quickFixParameters
        val ideaProject = idea.openProject(projectPath, projectName, moduleName)
        val results = idea.analyze(files, ideaProject)
        val reportStatus = if (reportParameters != null)
            reportProblems(idea, reportParameters, results) else true
        val quickFixStatus = if (quickFixParameters != null)
            quickFixProblems(quickFixParameters, projectName, moduleName, results) else true
        return reportStatus && quickFixStatus
    }

    private fun Application.analyze(files: Collection<File>, ideaProject: IdeaProject): Map<String, List<PinnedProblemDescriptor>> {
        logger.info("Before psi manager creation")
        val psiManager = PsiManager.getInstance(ideaProject)
        logger.info("Before virtual file manager creation")
        val virtualFileManager = VirtualFileManager.getInstance()
        val virtualFileSystem = virtualFileManager.getFileSystem("file")
        val documentManager = FileDocumentManager.getInstance()
        val fileIndex = ProjectFileIndex.getInstance(ideaProject)

        val results: MutableMap<String, MutableList<PinnedProblemDescriptor>> = mutableMapOf()
        logger.info("Before inspections launched: total of ${files.size} files to analyze")
        inspectionLoop@ for (inspectionWrapper in getInspectionWrappers(ideaProject)) {
            val inspectionClassName = inspectionWrapper.classFqName
            val inspectionTool = inspectionWrapper.tool
            when (inspectionTool) {
                is LocalInspectionTool -> {
                }
                is GlobalInspectionTool -> {
                    logger.info("Global inspection tools like $inspectionClassName are not yet supported")
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
                val task = Runnable {
                    try {
                        for (sourceFile in files) {
                            val filePath = sourceFile.absolutePath
                            val virtualFile = virtualFileSystem.findFileByPath(filePath)
                                    ?: throw InspectionRunnerException("Cannot find virtual file for $filePath")
                            if (!fileIndex.isInSource(virtualFile)) {
                                logger.warn("File $filePath is not in sources")
                                continue
                            }
                            val psiFile = psiManager.findFile(virtualFile)
                                    ?: throw InspectionRunnerException("Cannot find PSI file for $filePath")
                            if (!inspectionEnabledForFile(inspectionWrapper, psiFile)) continue
                            val document = documentManager.getDocument(virtualFile)
                                    ?: throw InspectionRunnerException("Cannot get document for $filePath")
                            inspectionResults += inspectionTool.analyze(psiFile, document, displayName, inspectionWrapper.level)
                        }
                    } catch (ie: InspectionException) {
                        logger.error("Exception during inspection running: " + ie.message)
                        logger.error("Caused by: " + ie.cause.message)
                        logger.error(ie.cause.stackTrace.joinToString(separator = "\n"))
                    }
                }
                ProgressManager.getInstance().runProcess(task, EmptyProgressIndicator())
            }
            results[inspectionClassName] = inspectionResults
        }
        return results
    }

    private fun reportProblems(idea: Application, parameters: ReportParameters, results: Map<String, List<PinnedProblemDescriptor>>): Boolean {
        var errors = 0
        var warnings = 0

        val generators = listOfNotNull(
                parameters.xmlReport?.let { XMLGenerator(it) },
                parameters.htmlReport?.let { HTMLGenerator(it, idea) }
        )

        var success = true
        val sortedResults = results.entries.map { entry -> entry.value.map { entry.key to it } }.flatten().sortedBy {
            val line = it.second.line
            val row = it.second.row
            (line shl 16) + row
        }.groupBy { it.second.fileName }
        logger.info("Total of ${sortedResults.values.flatten().count {
            val inspectionClass = it.first
            val problem = it.second
            problem.actualLevel(parameters.getLevel(inspectionClass)) != null
        }} problem(s) found")

        analysisLoop@ for (fileInspectionAndProblems in sortedResults.values) {
            for ((inspectionClass, problem) in fileInspectionAndProblems) {
                val level = problem.actualLevel(parameters.getLevel(inspectionClass)) ?: continue
                when (level) {
                    ProblemLevel.ERROR -> errors++
                    ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> warnings++
                    ProblemLevel.INFORMATION -> {
                    }
                }
                if (!parameters.quiet) {
                    log(level, problem.renderWithLocation())
                }
                generators.forEach { it.report(problem, level, inspectionClass) }
                if (errors > parameters.maxErrors) {
                    logger.error("Too many errors found: $errors. Analysis stopped")
                    success = false
                    break@analysisLoop
                }
                if (warnings > parameters.maxWarnings) {
                    logger.error("Too many warnings found: $warnings. Analysis stopped")
                    success = false
                    break@analysisLoop
                }
            }
        }
        generators.forEach { it.generate() }
        return success
    }

    private fun quickFixProblems(
            parameters: QuickFixParameters,
            projectName: String,
            moduleName: String,
            results: Map<String, List<PinnedProblemDescriptor>>
    ): Boolean {
        if (!parameters.hasQuickFix) return true
        val destination = parameters.destination
        val quickFixProjectPath = destination?.absolutePath ?: projectPath
        if (destination != null) {
            val source = File(projectPath)
            if (!destination.exists())
                destination.deleteRecursively()
            destination.mkdirs()
            source.copyTo(destination)
        }
        val project = idea.openProject(quickFixProjectPath, projectName, moduleName)
        WriteCommandAction.runWriteCommandAction(project) {
            for ((inspectionClassName, result) in results) {
                for (problem in result) {
                    val fixes = problem.fixes
                    if (fixes == null || fixes.size != 1) {
                        logger.error("Can not apply problem fixes to $problem")
                        continue
                    }
                    val fix = fixes[0]
                    fix.applyFix(project, problem)
                    logger.info("Fixed ${fix.name} for $inspectionClassName inspection")
                }
            }
        }
        return true
    }

    private fun ReportParameters.getLevel(clazz: String) = when (clazz) {
        in errors -> ProblemLevel.ERROR
        in warnings -> ProblemLevel.WARNING
        in infos -> ProblemLevel.INFORMATION
        else -> null
    }

    private fun log(level: ProblemLevel, s: String) {
        when (level) {
            ProblemLevel.INFORMATION -> logger.info(s)
            ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> logger.warn(s)
            ProblemLevel.ERROR -> logger.error(s)
        }
    }

    private fun inspectionEnabledForFile(wrapper: PluginInspectionWrapper, psiFile: PsiFile): Boolean =
            when (psiFile.language.displayName) {
                "Kotlin" -> wrapper.language in KOTLIN_FILE_APPLICABLE_LANGUAGES
                "Java" -> wrapper.language in JAVA_FILE_APPLICABLE_LANGUAGES
                else -> true
            }

    private class InspectionException(
            tool: LocalInspectionTool,
            file: PsiFile,
            override val cause: Throwable
    ) : Exception("Exception during ${tool.shortName} analysis of ${file.name}", cause)

    private fun LocalInspectionTool.analyze(
            file: PsiFile,
            document: Document,
            displayName: String?,
            problemLevel: ProblemLevel?
    ): List<PinnedProblemDescriptor> {
        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        try {
            val visitor = buildVisitor(holder, false, session)
            file.acceptRecursively(visitor)
        } catch (t: Throwable) {
            throw InspectionException(this, file, t)
        }
        return holder.results.map {
            PinnedProblemDescriptor(it, document, displayName, problemLevel)
        }
    }

    private fun Application.openProject(projectPath: String, projectName: String, moduleName: String): IdeaProject {
        logger.info("Before project creation at '$projectPath'")
        var ideaProject: IdeaProject? = null
        val projectFile = File(projectPath, projectName + ProjectFileType.DOT_DEFAULT_EXTENSION)
        invokeAndWait {
            ideaProject = ProjectUtil.openOrImport(
                    projectFile.absolutePath,
                    /* projectToClose = */ null,
                    /* forceOpenInNewFrame = */ true
            )
        }
        return ideaProject?.apply {
            val rootManager = ProjectRootManager.getInstance(this)
            logger.info("Project SDK name: " + rootManager.projectSdkName)
            logger.info("Project SDK: " + rootManager.projectSdk)

            val modules = ModuleManager.getInstance(this).modules.toList()
            for (module in modules) {
                if (module.name != moduleName) continue
                val moduleRootManager = ModuleRootManager.getInstance(module)
                val dependencyEnumerator =
                        moduleRootManager.orderEntries().compileOnly().recursively().exportedOnly()
                var dependsOnKotlinCommon = false
                var dependsOnKotlinJS = false
                var dependsOnKotlinJVM = false
                dependencyEnumerator.forEach { orderEntry ->
                    if (orderEntry is LibraryOrderEntry) {
                        val library = orderEntry.library
                        if (library != null) {
                            if (library.getUrls(OrderRootType.CLASSES).any { "$KT_LIB-common" in it }) {
                                dependsOnKotlinCommon = true
                            }
                            if (library.getUrls(OrderRootType.CLASSES).any { "$KT_LIB-js" in it }) {
                                dependsOnKotlinJS = true
                            }
                            if (library.getUrls(OrderRootType.CLASSES).any { "$KT_LIB-jdk" in it || "$KT_LIB-1" in it }) {
                                dependsOnKotlinJVM = true
                            }
                        }
                    }
                    true
                }
                when {
                    dependsOnKotlinJVM ->
                        logger.info("Under analysis: Kotlin JVM module $module with SDK: " + moduleRootManager.sdk)
                    dependsOnKotlinJS ->
                        logger.warn("Under analysis: Kotlin JS module $module (JS SDK is not supported yet)")
                    dependsOnKotlinCommon ->
                        logger.warn("Under analysis: Kotlin common module $module (common SDK is not supported yet)")
                    else ->
                        logger.info("Under analysis: pure Java module $module with SDK: " + moduleRootManager.sdk)
                }
            }
        } ?: run {
            throw InspectionRunnerException("Cannot open IDEA project: '${projectFile.absolutePath}'")
        }
    }
}