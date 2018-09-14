package org.jetbrains.intellij

import java.io.PrintWriter
import java.io.StringWriter

class Logger(private val level: LoggerLevel?, private val logger: (LoggerLevel, String) -> Unit) {

    private fun hasWritePermissions(level: LoggerLevel): Boolean {
        val writeLevel = this.level ?: return true
        return writeLevel.priority >= level.priority
    }

    fun error(message: Any? = "") {
        if (!hasWritePermissions(LoggerLevel.ERROR)) return
        logger(LoggerLevel.ERROR, message.toString())
    }

    fun warn(message: Any? = "") {
        if (!hasWritePermissions(LoggerLevel.WARNING)) return
        logger(LoggerLevel.WARNING, message.toString())
    }

    fun info(message: Any? = "") {
        if (!hasWritePermissions(LoggerLevel.INFO)) return
        logger(LoggerLevel.INFO, message.toString())
    }

    fun debug(message: Any? = "") {
        if (!hasWritePermissions(LoggerLevel.DEBUG)) return
        logger(LoggerLevel.DEBUG, message.toString())
    }

    fun exception(exception: Throwable) {
        val writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))
        error(writer.toString())
    }
}