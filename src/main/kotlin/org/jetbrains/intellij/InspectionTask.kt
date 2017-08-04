package org.jetbrains.intellij

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorUtil
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.jetbrains.idea.inspections.InspectionRunner
import com.intellij.openapi.project.Project as IdeaProject
import org.gradle.api.Project as GradleProject

class InspectionTask : SourceTask() {

    lateinit var sourceSet: SourceSet

    private val sourceDirs: FileCollection
        get() = project.files(sourceSet.allSource.srcDirs.filter { !sourceSet.resources.contains(it) && it.exists() })

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
            val files: MutableList<PsiFile> = mutableListOf()
            for (sourceDir in sourceDirs) {
                val virtualFile = virtualFileManager.findFileByUrl(sourceDir.absolutePath) ?: continue
                val psiDir = psiManager.findDirectory(virtualFile) ?: continue
                files += psiDir.files
            }
            val results: MutableMap<String, MutableList<ProblemDescriptor>> = mutableMapOf()
            for (inspectionClass in inspectionClasses) {
                val inspectionResults = mutableListOf<ProblemDescriptor>()
                val runner = InspectionRunner(inspectionClass)
                for (file in files) {
                    inspectionResults += runner.analyze(file)
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