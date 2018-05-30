package org.jetbrains.intellij

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

open class CleanTask : ConventionTask() {

    @TaskAction
    fun clean() {
        for (task in project.tasks) {
            if (task !is Inspection) continue
            task.reports.map { it.destination }.forEach {
                if (it.exists()) {
                    logger.info("Deleting report file: ${it.path}")
                    it.delete()
                } else {
                    logger.info("Touching report file: ${it.path}")
                    it.parentFile?.mkdirs()
                    it.createNewFile()
                }
            }
        }
    }
}