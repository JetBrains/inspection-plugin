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
import org.jetbrains.intellij.InspectionClassesSuite

class InspectionRunner(
        private val maxErrors: Int,
        private val maxWarnings: Int,
        private val showViolations: Boolean,
        private val inspectionClasses: InspectionClassesSuite
) {
    private val ideaProjectManager = ProjectManager.getInstance()
    private val ideaProject = ideaProjectManager.defaultProject // FIXME
    private val psiManager = PsiManager.getInstance(ideaProject)
    private val virtualFileManager = VirtualFileManager.getInstance()

    private fun ProblemDescriptor.level(default: LogLevel): LogLevel = when (highlightType) {
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
        for ((inspectionClass, problems) in results) {
            for (problem in problems) {
                val level = problem.level(inspectionClasses.getLevel(inspectionClass))
                when (level) {
                    LogLevel.ERROR -> errors++
                    LogLevel.WARN -> warnings++
                    else -> {}
                }
                if (showViolations) {
                    logger.log(level, problem.render())
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
    }

    private fun analyzeTree(tree: FileTree): Map<String, List<ProblemDescriptor>> {
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