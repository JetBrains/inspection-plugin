package org.jetbrains.intellij

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.jetbrains.idea.inspections.InspectionRunner
import org.slf4j.LoggerFactory
import com.intellij.openapi.project.Project as IdeaProject
import org.gradle.api.Project as GradleProject

class InspectionTask : DefaultTask() {

    private val log = LoggerFactory.getLogger(this::class.java)

    lateinit var sourceSet: SourceSet

    private val sourceDirs: FileCollection
        get() = project.files(sourceSet.allSource.srcDirs.filter { !sourceSet.resources.contains(it) && it.exists() })

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
            // TODO: do something with results :)
        }
        catch (e: Throwable) {
            log.error(e.message)
            throw TaskExecutionException(this, Exception("Exception occurred in analyze task", e))
        }
    }
}