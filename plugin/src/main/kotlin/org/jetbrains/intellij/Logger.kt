package org.jetbrains.intellij

import java.util.function.BiFunction

open class Logger(private val logger: BiFunction<Int, String, Unit>) {

    private enum class LoggingLevel(val level: Int) {
        ERROR(0),
        WARNING(1),
        INFO(2)
    }

    fun info(s: Any? = "") {
        logger.apply(LoggingLevel.INFO.level, s.toString())
    }

    fun warn(s: Any? = "") {
        logger.apply(LoggingLevel.WARNING.level, s.toString())
    }

    fun error(s: Any? = "") {
        logger.apply(LoggingLevel.ERROR.level, s.toString())
    }
}