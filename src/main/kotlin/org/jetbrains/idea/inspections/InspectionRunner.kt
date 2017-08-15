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
import org.jetbrains.intellij.InspectionPluginExtension

class InspectionRunner(
        private vararg val inspectionClasses: String
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

    fun analyzeTreeAndLogResults(tree: FileTree, extension: InspectionPluginExtension, logger: Logger) {
        logger.info("Input classes: " + inspectionClasses.toList().toString())
        val results = analyzeTree(tree)
        for ((inspectionClass, problems) in results) {
            for (problem in problems) {
                logger.log(problem.level(extension.getLevel(inspectionClass)), problem.render())
            }
        }
    }

    private fun analyzeTree(tree: FileTree): Map<String, List<ProblemDescriptor>> {
        val results: MutableMap<String, MutableList<ProblemDescriptor>> = mutableMapOf()
        for (inspectionClass in inspectionClasses) {
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