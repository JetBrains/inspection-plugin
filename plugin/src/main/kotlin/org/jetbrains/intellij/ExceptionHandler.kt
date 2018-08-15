package org.jetbrains.intellij

import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskExecutionException

object ExceptionHandler {
    private fun hasStackTraceParameter() = true

    fun exception(logger: Logger, task: Task, message: String): Nothing {
        logger.error("InspectionPlugin: $message")
        val exception = Exception(message)
        throw TaskExecutionException(task, exception)
    }

    fun exception(logger: Logger, task: Task, throwable: Throwable, message: String): Nothing {
        logger.error("InspectionPlugin: $message")
        when {
            hasStackTraceParameter() -> printStackTrace(logger, throwable)
            else -> printNameStackTrace(logger, throwable)
        }
        throw TaskExecutionException(task, throwable)
    }

    private fun printNameStackTrace(logger: Logger, throwable: Throwable, indent: Int = 1) {
        val prefix = "    ".repeat(indent)
        logger.error(prefix + throwable.message)
        throwable.cause?.let { printNameStackTrace(logger, it, indent + 1) }
    }

    private fun printStackTrace(logger: Logger, throwable: Throwable) {
        logger.error("Caused by: $throwable")
        logger.error(throwable.stackTrace.joinToString(separator = "\n") { "    $it" })
        throwable.cause?.let { printStackTrace(logger, it) }
    }
}