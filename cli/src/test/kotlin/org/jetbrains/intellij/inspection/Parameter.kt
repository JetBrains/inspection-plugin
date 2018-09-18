package org.jetbrains.intellij.inspection

import org.jetbrains.intellij.LoggerLevel
import java.io.File

object Parameter {
    fun isNull(value: String) = value == "null"

    fun parseFile(value: String): File {
        return File(value.removeSurrounding("File(\"", "\")"))
    }

    inline operator fun <reified T> invoke(value: String?): T? = value?.let { Parameter.invoke<T>(it) }

    inline operator fun <reified T> invoke(value: String?, crossinline action: (T) -> Unit): Unit? =
            value?.let { Parameter.invoke<T>(it) }?.let { action(it) }

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

    @JvmName("valueOf")
    inline operator fun <reified T> invoke(value: String): T? = when (T::class) {
        List::class -> if (isNull(value)) null else parseListOfStrings(value) as T
        LoggerLevel::class -> if (isNull(value)) null else parseLoggerLevel(value) as T
        File::class -> if (isNull(value)) null else parseFile(value) as T
        Set::class -> if (isNull(value)) null else parseSetOfStrings(value) as T
        Int::class -> if (isNull(value)) null else value.toInt() as T
        Boolean::class -> if (isNull(value)) null else value.toBoolean() as T
        else -> throw IllegalArgumentException("Unexpected type parameter ${T::class}")
    }
}