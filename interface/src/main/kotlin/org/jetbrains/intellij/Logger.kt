package org.jetbrains.intellij

import java.io.PrintWriter
import java.io.StringWriter

class Logger(private val level: LoggerLevel, prefix: String? = null) {

    private val prefix = prefix?.let { "$prefix: " } ?: ""

    fun proxy(level: LoggerLevel, message: String) = when (level) {
        LoggerLevel.INFO -> info("$prefix$message")
        LoggerLevel.WARNING -> warn("$prefix$message")
        LoggerLevel.ERROR -> error("$prefix$message")
    }

    fun error(message: Any? = "") {
        if (level.priority < LoggerLevel.ERROR.priority) return
        System.err.println(message)
    }

    fun warn(message: Any? = "") {
        if (level.priority < LoggerLevel.WARNING.priority) return
        System.out.println(message)
    }

    fun info(message: Any? = "") {
        if (level.priority < LoggerLevel.INFO.priority) return
        System.out.println(message)
    }

    fun exception(exception: Throwable) {
        val writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))
        error(writer.toString())
    }
}