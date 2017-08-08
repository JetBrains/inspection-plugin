package org.jetbrains.intellij

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorUtil
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.jetbrains.idea.inspections.InspectionRunner
import com.intellij.openapi.project.Project as IdeaProject
import org.gradle.api.Project as GradleProject

open class InspectionTask : SourceTask() {

    private fun ProblemDescriptor.level(default: LogLevel): LogLevel {
        return when (highlightType) {
            ProblemHighlightType.ERROR, ProblemHighlightType.GENERIC_ERROR -> LogLevel.ERROR
            ProblemHighlightType.WEAK_WARNING -> LogLevel.WARN
            ProblemHighlightType.INFORMATION -> LogLevel.INFO
            else -> default
        }
    }

    private fun ProblemDescriptor.render() =
            StringUtil.replace(
                    StringUtil.replace(
                            descriptionTemplate,
                            "#ref",
                            psiElement?.let { ProblemDescriptorUtil.extractHighlightedText(this, it) } ?: ""
                    ), " #loc ", " ")

    @TaskAction
    fun analyze() {
        try {
            val extension = project.extensions.findByType(InspectionPluginExtension::class.java)
            val inspectionClasses = extension.inspectionClasses
            val ideaProjectManager = ProjectManager.getInstance()
            val ideaProject = ideaProjectManager.defaultProject // FIXME
            val psiManager = PsiManager.getInstance(ideaProject)
            val virtualFileManager = VirtualFileManager.getInstance()
            val results: MutableMap<String, MutableList<ProblemDescriptor>> = mutableMapOf()
            for (inspectionClass in inspectionClasses) {
                val inspectionResults = mutableListOf<ProblemDescriptor>()
                val runner = InspectionRunner(inspectionClass)
                for (sourceFile in getSource()) {
                    val virtualFile = virtualFileManager.findFileByUrl(sourceFile.absolutePath) ?: continue
                    val psiFile = psiManager.findFile(virtualFile) ?: continue
                    inspectionResults += runner.analyze(psiFile)
                }
                results[inspectionClass] = inspectionResults
            }
            for ((inspectionClass, problems) in results) {
                for (problem in problems) {
                    logger.log(problem.level(extension.getLevel(inspectionClass)), problem.render())
                }
            }
        }
        catch (e: Throwable) {
            logger.error(e.message)
            throw TaskExecutionException(this, Exception("Exception occurred in analyze task", e))
        }
    }
}