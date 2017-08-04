package org.jetbrains.intellij

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.jetbrains.idea.inspections.InspectionRunner
import org.slf4j.LoggerFactory

class InspectionTask : DefaultTask() {

    private val log = LoggerFactory.getLogger(this::class.java)

    @TaskAction
    fun analyze() {
        try {
            val extension = project.extensions.findByType(InspectionPluginExtension::class.java)
            val inspectionClasses = extension.inspectionClasses
            for (inspectionClass in inspectionClasses) {
                val runner = InspectionRunner(inspectionClass)
                // ...
            }
        }
        catch (e: Throwable) {
            log.error(e.message)
            throw TaskExecutionException(this, Exception("Exception occurred in analyze task", e))
        }
    }
}