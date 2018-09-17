package org.jetbrains.intellij.inspection

import org.jetbrains.intellij.LoggerLevel
import java.io.File

data class ToolArguments(
        val errors: List<String>?,
        val warnings: List<String>?,
        val info: List<String>?,
        val tasks: List<String>?,
        val level: LoggerLevel?,
        val config: File?,
        val runner: File?,
        val idea: File?,
        val project: File?
) {
    fun toCommandLineArguments() = tasks!! + listOfNotNull(
            when (level) {
                LoggerLevel.ERROR -> "--error"
                LoggerLevel.WARNING -> "--warning"
                LoggerLevel.INFO -> "--info"
                LoggerLevel.DEBUG -> "--debug"
                null -> null
            },
            config?.let { "--config=${it.absolutePath}" },
            runner?.let { "--runner=${it.absolutePath}" },
            idea?.let { "--idea=${it.absolutePath}" },
            project?.let { "--project=${it.absolutePath}" }
    )
}