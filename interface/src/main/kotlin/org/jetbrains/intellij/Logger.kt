package org.jetbrains.intellij

open class Logger() {

    enum class LoggingLevel {
        ERROR, WARNING, INFO;

        val prefix by lazy {
            "($name): "
        }

        companion object {
            fun ejectLevel(message: String) = when {
                message.startsWith(ERROR.prefix) -> Pair(ERROR, message.removePrefix(ERROR.prefix))
                message.startsWith(WARNING.prefix) -> Pair(WARNING, message.removePrefix(WARNING.prefix))
                message.startsWith(INFO.prefix) -> Pair(INFO, message.removePrefix(INFO.prefix))
                else -> Pair(null, message)
            }
        }
    }

    fun info(message: Any? = "") {
        println(LoggingLevel.INFO.prefix + message)
    }

    fun warn(message: Any? = "") {
        println(LoggingLevel.WARNING.prefix + message)
    }

    fun error(message: Any? = "") {
        println(LoggingLevel.ERROR.prefix + message)
    }
}