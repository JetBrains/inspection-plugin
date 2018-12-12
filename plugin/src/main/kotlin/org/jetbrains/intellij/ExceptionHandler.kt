package org.jetbrains.intellij

import org.gradle.api.Task
import org.gradle.api.tasks.TaskExecutionException

fun exception(task: Task, message: String): Nothing {
    task.logger.error("InspectionPlugin: $message")
    val exception = Exception(message)
    throw TaskExecutionException(task, exception)
}
