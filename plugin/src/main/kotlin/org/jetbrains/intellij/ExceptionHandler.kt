package org.jetbrains.intellij

import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskExecutionException

object ExceptionHandler {
    fun handle(logger: Logger, task: Task, exception: Throwable, message: String): Nothing {
        logger.error("$message $exception")
        logger.error(exception.stackTrace.joinToString(separator = "\n") { "    $it" })
        exception.cause?.let {
            logger.error("Caused by: " + (it.message ?: it))
            logger.error(it.stackTrace.joinToString(separator = "\n") { line -> "    $line" })
        }
        throw TaskExecutionException(task, exception)
    }
}