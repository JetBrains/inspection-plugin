package org.jetbrains.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.createCommandLineApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.PlatformUtils
import org.gradle.api.GradleException
import org.gradle.api.Project as GradleProject
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.jdom2.Document as JdomDocument
import org.jdom2.Element
import org.jdom2.output.XMLOutputter
import org.jetbrains.intellij.*
import java.io.File
import com.intellij.openapi.editor.Document as IdeaDocument
import java.lang.Exception

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

        private val USELESS_PLUGINS = listOf(
                "mobi.hsz.idea.gitignore",
                "org.jetbrains.plugins.github",
                "Git4Idea"
        )
    }

    private val projectPath: String = project.rootProject.projectDir.absolutePath

    private fun PinnedProblemDescriptor.actualLevel(default: ProblemLevel?): ProblemLevel? = when (highlightType) {
        // Default (use level either from IDEA configuration or plugin configuration)
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
        ProblemHighlightType.LIKE_DEPRECATED,
        ProblemHighlightType.LIKE_UNUSED_SYMBOL ->
            default ?: this.ideaLevel
        // If inspection forces error, report it
        ProblemHighlightType.ERROR, ProblemHighlightType.GENERIC_ERROR ->
            ProblemLevel.ERROR
        // If inspection forces weak warning, never report error
        ProblemHighlightType.WEAK_WARNING ->
            if (default == ProblemLevel.ERROR || default == null) ProblemLevel.WEAK_WARNING else default
        // If inspection forces "do not show", it's really not a problem at all
        ProblemHighlightType.INFORMATION ->
            null
        else /* INFO only */ ->
            ProblemLevel.INFORMATION
    }

    // Returns true if analysis executed successfully
    override fun analyzeTreeAndLogResults(tree: FileTree): Boolean {
        logger.info("Class loader: " + this.javaClass.classLoader)
        logger.info("Input classes: " + inspectionClasses)
        val results = analyzeTreeInIdea(tree)
        var errors = 0
        var warnings = 0

        val xmlReport = reports.xml
        val xmlEnabled = xmlReport.isEnabled
        val xmlRoot = Element("report")
        val errorsRoot = Element("errors")
        val errorElements = mutableListOf<Element>()
        val warningsRoot = Element("warnings")
        val warningElements = mutableListOf<Element>()
        val infosRoot = Element("infos")
        val infoElements = mutableListOf<Element>()

        var success = true
        logger.info("Total of ${results.values.flatten().size} problems found")
        analysisLoop@ for ((inspectionClass, problems) in results) {
            for (problem in problems) {
                val level = problem.actualLevel(inspectionClasses.getLevel(inspectionClass)) ?: continue
                when (level) {
                    ProblemLevel.ERROR -> errors++
                    ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> warnings++
                    ProblemLevel.INFORMATION -> {}
                }
                if (!quiet) {
                    logger.log(level.logLevel, problem.renderWithLocation())
                }
                if (xmlEnabled) {
                    val element = Element("problem")
                    element.addContent(Element("file").addContent(problem.fileName))
                    element.addContent(Element("line").addContent((problem.line + 1).toString()))
                    element.addContent(Element("row").addContent((problem.row + 1).toString()))
                    element.addContent(Element("java_class").addContent(inspectionClass))
                    element.addContent(Element("problem_class")
                            .setAttribute("severity", level.name)
                            .addContent(problem.displayName ?: "<ANONYMOUS>"))
                    element.addContent(Element("description").addContent(problem.render()))
                    when (level) {
                        ProblemLevel.ERROR -> errorElements += element
                        ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> warningElements += element
                        ProblemLevel.INFORMATION -> infoElements += element
                    }
                }
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

        val xmlReportFile = if (!xmlEnabled) null else xmlReport.destination
        if (xmlReportFile != null) {
            errorsRoot.setContent(errorElements)
            warningsRoot.setContent(warningElements)
            infosRoot.setContent(infoElements)
            xmlRoot.addContent(errorsRoot)
            xmlRoot.addContent(warningsRoot)
            xmlRoot.addContent(infosRoot)
            val document = JdomDocument(xmlRoot)
            XMLOutputter().output(document, xmlReportFile.outputStream())
        }

        return success
    }

    private fun analyzeTreeInIdea(tree: FileTree): Map<String, List<PinnedProblemDescriptor>> {
        System.setProperty(IDEA_HOME_PATH, UnzipTask.cacheDirectory.path)
        System.setProperty(AWT_HEADLESS, "true")
        System.setProperty(BUILD_NUMBER, UnzipTask.buildNumber())

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX))
        logger.warn("IDEA home path: " + PathManager.getHomePath())
        createCommandLineApplication(isInternal = false, isUnitTestMode = false, isHeadless = true)
        for (plugin in USELESS_PLUGINS) {
            PluginManagerCore.disablePlugin(plugin)
        }

        logger.info("Plugins enabled: " + PluginManagerCore.getPlugins().toList())
        ApplicationManagerEx.getApplicationEx().load()
        val application = ApplicationManagerEx.getApplicationEx() ?: run {
            throw GradleException("Cannot create IDEA application")
        }
        val result: Map<String, List<PinnedProblemDescriptor>>?
        try {
            application.doNotSave()

            result = application.analyzeTree(tree)
        } catch (e: Exception) {
            if (e is GradleException) throw e
            throw GradleException("EXCEPTION caught in inspection plugin (IDEA runReadAction): " + e, e)
        } finally {
            application.exit(true, true)
        }
        return result ?: emptyMap()
    }

    private class PluginInspectionWrapper(
            val tool: InspectionProfileEntry,
            val extension: InspectionEP?,
            val classFqName: String,
            useInspectionEPLevel: Boolean = false
    ) {
        val level: ProblemLevel? =
                if (useInspectionEPLevel) ProblemLevel.fromInspectionEPLevel(extension?.level) else null
    }

    private fun InspectionClassesSuite.getInspectionWrappers(
            tools: List<InspectionToolWrapper<InspectionProfileEntry, InspectionEP>>,
            ideaProject: IdeaProject
    ): Sequence<PluginInspectionWrapper> {
        if (inheritFromIdea) {
            val inspectionProjectProfileManager = InspectionProjectProfileManager.getInstance(ideaProject)
            val inspectionProfile = inspectionProjectProfileManager.currentProfile
            val profileTools = inspectionProfile.getAllEnabledInspectionTools(ideaProject)
            return profileTools.asSequence().map {
                val wrapper = it.tool
                PluginInspectionWrapper(wrapper.tool, wrapper.extension, wrapper.tool.javaClass.name,
                        useInspectionEPLevel = true)
            }
        }
        return classes.asSequence().mapNotNull { inspectionClassName ->
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
        val tools = InspectionToolRegistrar.getInstance().createTools()
        inspectionLoop@ for (inspectionWrapper in inspectionClasses.getInspectionWrappers(tools, ideaProject)) {
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

    private fun PsiElement.acceptRecursively(visitor: PsiElementVisitor) {
        this.accept(visitor)
        for (child in this.children) {
            child.acceptRecursively(visitor)
        }
    }

    private class PinnedProblemDescriptor(
            val descriptor: ProblemDescriptor,
            val fileName: String,
            val line: Int,
            val row: Int,
            val displayName: String?,
            val ideaLevel: ProblemLevel?
    ) : ProblemDescriptor by descriptor {
        private val highlightedText = psiElement?.let {
            ProblemDescriptorUtil.extractHighlightedText(this, it)
        } ?: ""

        fun renderLocation(): String = StringBuilder().apply {
            append(fileName)
            append(":")
            append(line + 1)
            append(":")
            append(row + 1)
        }.toString()

        fun renderWithLocation(): String = renderLocation() + ": " + render()

        fun render(): String = StringUtil.replace(
                StringUtil.replace(
                        descriptionTemplate,
                        "#ref",
                        highlightedText
                ),
                " #loc",
                ""
        )

        constructor(descriptor: ProblemDescriptor, document: IdeaDocument, displayName: String?,
                    problemLevel: ProblemLevel?,
                    lineNumber: Int = document.getLineNumber(descriptor.psiElement.textRange.startOffset)):
                this(descriptor, descriptor.psiElement.containingFile.name, lineNumber,
                     descriptor.psiElement.textRange.startOffset - document.getLineStartOffset(lineNumber),
                     displayName, problemLevel)
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