package org.jetbrains.intellij

import java.io.File

object Parameter {
    fun isNull(value: String) = value == "null"

    fun parseFile(value: String): File {
        return File(value.removeSurrounding("File(\"", "\")"))
    }

    fun parseString(value: String): String {
        return value.removeSurrounding("\"")
    }

    fun parseListOfStrings(value: String): List<String> {
        val internal = value.removeSurrounding("listOf(", ")")
        return internal.split(",").asSequence().map { it.trim().removeSurrounding("\"") }.toList()
    }

    fun parseLoggerLevel(value: String): LoggerLevel {
        return when (value) {
            "error" -> LoggerLevel.ERROR
            "warn", "warning" -> LoggerLevel.WARNING
            "info" -> LoggerLevel.INFO
            "debug" -> LoggerLevel.DEBUG
            else -> throw IllegalArgumentException("Logger level: $value not recognized")
        }
    }

    fun parseSetOfStrings(value: String): Set<String> {
        val internal = value.removeSurrounding("setOf(", ")")
        return internal.split(",").asSequence().map { it.trim().removeSurrounding("\"") }.toSet()
    }

    inline operator fun <reified T> invoke(value: String?): T? = value?.let { invoke<T>(it) }

    inline operator fun <reified T> invoke(value: String?, crossinline action: (T) -> Unit): Unit? =
            value?.let { invoke<T>(it) }?.let { action(it) }

    @JvmName("valueOf")
    inline operator fun <reified T> invoke(value: String): T? {
        if (isNull(value)) return null
        return when (T::class) {
            String::class ->  parseString(value) as T
            File::class -> parseFile(value) as T
            Set::class -> parseSetOfStrings(value) as T
            Int::class -> value.toInt() as T
            Boolean::class -> value.toBoolean() as T
            List::class -> parseListOfStrings(value) as T
            LoggerLevel::class -> parseLoggerLevel(value) as T
            else -> throw IllegalArgumentException("Unexpected type parameter ${T::class}")
        }
    }
}