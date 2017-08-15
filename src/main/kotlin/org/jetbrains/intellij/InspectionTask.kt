package org.jetbrains.intellij

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.jetbrains.idea.inspections.InspectionRunner
import org.gradle.api.Project as GradleProject

open class InspectionTask : SourceTask() {

    @TaskAction
    fun analyze() {
        try {
            val extension = project.extensions.findByType(InspectionPluginExtension::class.java)
            val inspectionClasses = extension.inspectionClasses

            val runner = InspectionRunner(*inspectionClasses)
            runner.analyzeTreeAndLogResults(getSource(), extension, logger)
        }
        catch (e: Throwable) {
            logger.error(e.message)
            throw TaskExecutionException(this, Exception("Exception occurred in analyze task", e))
        }
    }
}