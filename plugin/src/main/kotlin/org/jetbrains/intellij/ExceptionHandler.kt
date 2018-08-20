package org.jetbrains.intellij

import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskExecutionException

object ExceptionHandler {

    // TODO check gradle parameters
    private fun hasStackTraceParameter() = true

    fun exception(task: Task, message: String): Nothing {
        task.logger.error("InspectionPlugin: $message")
        val exception = Exception(message)
        throw TaskExecutionException(task, exception)
    }

    fun exception(task: Task, throwable: Throwable, message: String, beforeThrow: () -> Unit = {}): Nothing {
        task.logger.error("InspectionPlugin: $message")
        when {
            hasStackTraceParameter() -> printStackTrace(task, throwable)
            else -> printNameStackTrace(task, throwable)
        }
        beforeThrow()
        throw TaskExecutionException(task, throwable)
    }

    private fun printNameStackTrace(task: Task, throwable: Throwable, indent: Int = 1) {
        val prefix = "    ".repeat(indent)
        task.logger.error(prefix + throwable.message)
        throwable.cause?.let { printNameStackTrace(task, it, indent + 1) }
    }

    private fun printStackTrace(task: Task, throwable: Throwable) {
        task.logger.error("Caused by: $throwable")
        task.logger.error(throwable.stackTrace.joinToString(separator = "\n") { "    $it" })
        throwable.cause?.let { printStackTrace(task, it) }
    }
}