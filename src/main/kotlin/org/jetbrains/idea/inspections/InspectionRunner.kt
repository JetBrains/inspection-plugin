package org.jetbrains.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.createCommandLineApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.PlatformUtils
import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.jdom2.Document as JdomDocument
import org.jdom2.Element
import org.jdom2.output.XMLOutputter
import org.jetbrains.intellij.IdeaCheckstyleReports
import org.jetbrains.intellij.InspectionClassesSuite
import org.jetbrains.intellij.UnzipTask
import java.io.File
import com.intellij.openapi.editor.Document as IdeaDocument

class InspectionRunner(
        private val projectDir: File,
        private val maxErrors: Int,
        private val maxWarnings: Int,
        private val showViolations: Boolean,
        private val inspectionClasses: InspectionClassesSuite,
        private val reports: IdeaCheckstyleReports
) {
    companion object {
        private val AWT_HEADLESS = "java.awt.headless"
        private val IDEA_HOME_PATH = "idea.home.path"
        private val BUILD_NUMBER = "idea.plugins.compatible.build"
    }

    private val projectPath: String = projectDir.absolutePath

    private fun ProblemDescriptor.level(default: LogLevel?): LogLevel? = when (highlightType) {
        ProblemHighlightType.ERROR, ProblemHighlightType.GENERIC_ERROR -> LogLevel.ERROR
        ProblemHighlightType.WEAK_WARNING -> LogLevel.WARN
        ProblemHighlightType.INFORMATION -> LogLevel.INFO
        else -> default
    }

    // Returns true if analysis executed successfully
    fun analyzeTreeAndLogResults(tree: FileTree, logger: Logger): Boolean {
        logger.info("Input classes: " + inspectionClasses)
        val results = analyzeTreeInIdea(tree, logger)
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
                val level = problem.level(inspectionClasses.getLevel(inspectionClass)) ?: continue
                when (level) {
                    LogLevel.ERROR -> errors++
                    LogLevel.WARN -> warnings++
                    else -> {}
                }
                val renderedText = problem.render()
                if (showViolations) {
                    logger.log(level, renderedText)
                }
                if (xmlEnabled) {
                    val element = Element(when (level) {
                        LogLevel.ERROR -> "error"
                        LogLevel.WARN -> "warning"
                        else -> "info"
                    })
                    element.setAttribute("class", inspectionClass)
                    element.setAttribute("text", renderedText)
                    element.setAttribute("file", problem.psiElement.containingFile.name)
                    element.setAttribute("line", (problem.line + 1).toString())
                    when (level) {
                        LogLevel.ERROR -> errorElements += element
                        LogLevel.WARN -> warningElements += element
                        else -> infoElements += element
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

        val xmlReportFile = xmlReport.destination.takeIf { xmlEnabled }
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

    private fun analyzeTreeInIdea(tree: FileTree, logger: Logger): Map<String, List<PinnedProblemDescriptor>> {
        System.setProperty(IDEA_HOME_PATH, UnzipTask.cacheDirectory.path)
        System.setProperty(AWT_HEADLESS, "true")
        System.setProperty(BUILD_NUMBER, UnzipTask.buildNumber())

        System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.getPlatformPrefix(PlatformUtils.IDEA_CE_PREFIX))
        logger.warn("IDEA home path: " + PathManager.getHomePath())
        createCommandLineApplication(isInternal = false, isUnitTestMode = false, isHeadless = true)
        PluginManagerCore.addPluginClass(PluginId.getId("org.jetbrains.kotlin"))
        PluginManagerCore.enablePlugin("Kotlin")
        logger.info("Plugins enabled: " + PluginManagerCore.getPlugins().toList())
        ApplicationManagerEx.getApplicationEx().load()
        val application = ApplicationManagerEx.getApplicationEx() ?: run {
            throw GradleException("Cannot create IDEA application")
        }
        val result: Map<String, List<PinnedProblemDescriptor>>?
        try {
            application.doNotSave()

            result = application.analyzeTree(tree, logger)
        } catch (e: Exception) {
            if (e is GradleException) throw e
            throw GradleException("EXCEPTION caught in inspection plugin (IDEA runReadAction): " + e, e)
        } finally {
            application.exit(true, true)
        }
        return result ?: emptyMap()
    }

    private fun Application.analyzeTree(tree: FileTree, logger: Logger): Map<String, List<PinnedProblemDescriptor>> {
        logger.info("Before project creation at '$projectPath'")
        val ideaProject: Project = run {
            var project: Project? = null
            val projectFileName = projectDir.name + ".ipr"
            val projectFile = File(projectPath, projectFileName)
            invokeAndWait {
                project = ProjectUtil.openOrImport(projectFile.absolutePath, null, false)
            }
            project ?: run {
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
        for (inspectionClass in inspectionClasses.classes) {
            @Suppress("UNCHECKED_CAST")
            val inspectionTool = (Class.forName(inspectionClass) as Class<LocalInspectionTool>).newInstance()
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
                    inspectionResults += inspectionTool.analyze(psiFile, document)
                }
            }
            results[inspectionClass] = inspectionResults
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
            val row: Int
    ) : ProblemDescriptor by descriptor {
        private val highlightedText = psiElement?.let {
            ProblemDescriptorUtil.extractHighlightedText(this, it)
        } ?: ""

        fun renderLocation(): String {
            return StringBuilder().apply {
                append(fileName)
                append(":")
                append(line + 1)
                append(":")
                append(row + 1)
            }.toString()
        }

        fun render(): String {
            return renderLocation() + ": " + StringUtil.replace(
                    StringUtil.replace(
                            descriptionTemplate,
                            "#ref",
                            highlightedText
                    ),
                    " #loc ",
                    " "
            )
        }

        constructor(descriptor: ProblemDescriptor, document: IdeaDocument,
                    lineNumber: Int = document.getLineNumber(descriptor.psiElement.textRange.startOffset)):
                this(descriptor,
                     descriptor.psiElement.containingFile.name,
                     lineNumber,
                     descriptor.psiElement.textRange.startOffset - document.getLineStartOffset(lineNumber))
    }

    private fun LocalInspectionTool.analyze(file: PsiFile, document: IdeaDocument): List<PinnedProblemDescriptor> {
        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val session = LocalInspectionToolSession(file, file.textRange.startOffset, file.textRange.endOffset)
        val visitor = this.buildVisitor(holder, false, session)
        file.acceptRecursively(visitor)
        return holder.results.map {
            PinnedProblemDescriptor(it, document)
        }
    }
}