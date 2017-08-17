package org.jetbrains.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jdom.Document
import org.jdom.Element
import org.jdom.output.XMLOutputter
import org.jetbrains.intellij.IdeaCheckstyleReports
import org.jetbrains.intellij.InspectionClassesSuite

class InspectionRunner(
        private val maxErrors: Int,
        private val maxWarnings: Int,
        private val showViolations: Boolean,
        private val inspectionClasses: InspectionClassesSuite,
        private val reports: IdeaCheckstyleReports
) {
    private val logger = Logging.getLogger(InspectionRunner::class.java)

    private fun ProblemDescriptor.level(default: LogLevel?): LogLevel? = when (highlightType) {
        ProblemHighlightType.ERROR, ProblemHighlightType.GENERIC_ERROR -> LogLevel.ERROR
        ProblemHighlightType.WEAK_WARNING -> LogLevel.WARN
        ProblemHighlightType.INFORMATION -> LogLevel.INFO
        else -> default
    }

    private fun ProblemDescriptor.render() =
            StringUtil.replace(
                    StringUtil.replace(
                            descriptionTemplate,
                            "#ref",
                            psiElement?.let { ProblemDescriptorUtil.extractHighlightedText(this, it) } ?: ""
                    ), " #loc ", " ")

    fun analyzeTreeAndLogResults(tree: FileTree, logger: Logger) {
        logger.info("Input classes: " + inspectionClasses.classes)
        val results = analyzeTree(tree)
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

        for ((inspectionClass, problems) in results) {
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
                    element.setAttribute("line", problem.lineNumber.toString())
                    when (level) {
                        LogLevel.ERROR -> errorElements += element
                        LogLevel.WARN -> warningElements += element
                        else -> infoElements += element
                    }
                }
                if (errors >= maxErrors) {
                    logger.error("Too many errors found: $errors. Analysis stopped")
                    return
                }
                if (warnings >= maxWarnings) {
                    logger.error("Too many warnings found: $warnings. Analysis stopped")
                    return
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
            val document = Document(xmlRoot)
            XMLOutputter().output(document, xmlReportFile.outputStream())
        }
    }

    private fun analyzeTree(tree: FileTree): Map<String, List<ProblemDescriptor>> {
        logger.info("Before project manager creation")
        val ideaProjectManager = ProjectManager.getInstance()
        logger.info("Before project creation")
        val ideaProject = ideaProjectManager.defaultProject // FIXME
        logger.info("Before psi manager creation")
        val psiManager = PsiManager.getInstance(ideaProject)
        logger.info("Before virtual file manager creation")
        val virtualFileManager = VirtualFileManager.getInstance()

        val results: MutableMap<String, MutableList<ProblemDescriptor>> = mutableMapOf()
        for (inspectionClass in inspectionClasses.classes) {
            @Suppress("UNCHECKED_CAST")
            val inspectionTool = (Class.forName(inspectionClass) as Class<LocalInspectionTool>).newInstance()
            val inspectionResults = mutableListOf<ProblemDescriptor>()
            for (sourceFile in tree) {
                val virtualFile = virtualFileManager.findFileByUrl(sourceFile.absolutePath) ?: continue
                val psiFile = psiManager.findFile(virtualFile) ?: continue
                inspectionResults += inspectionTool.analyze(psiFile)
            }
            results[inspectionClass] = inspectionResults
        }
        return results
    }

    private fun LocalInspectionTool.analyze(file: PsiFile): List<ProblemDescriptor> {
        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val visitor = this.buildVisitor(holder, false)
        visitor.visitFile(file)
        return holder.results
    }
}