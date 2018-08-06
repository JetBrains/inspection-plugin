package org.jetbrains.intellij.inspection

import java.io.File

object Parameter {
    fun isNull(value: String) = value == "null"

    fun parseFile(value: String): File {
        return value.removeSurroundingOrNull("File(\"", "\")")?.let(::File)
                ?: throw IllegalArgumentException("'$value' is not a file")
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun String.removeSurroundingOrNull(prefix: String, suffix: String = prefix): String? {
        return this.removeSurrounding(prefix, suffix).let { if(it == this) null else it }
    }

    fun parseString(value: String): String {
        return value.removeSurrounding("\"")
    }

    fun parseSetOfStrings(value: String): Set<String> {
        val internal = value.removeSurroundingOrNull("setOf(", ")")
                ?: throw IllegalArgumentException("'$value' is not a set")
        return internal.split(",").map { parseString(it.trim()) }.toSet()
    }

    inline operator fun <reified T> invoke(value: String?): T? = value?.let { Parameter.invoke<T>(it) }

    inline operator fun <reified T> invoke(value: String?, crossinline action: (T) -> Unit): Unit? =
            value?.let { Parameter.invoke<T>(it) }?.let { action(it) }

    @JvmName("valueOf")
    inline operator fun <reified T> invoke(value: String): T? = when (T::class) {
        String::class -> if (isNull(value)) null else parseString(value) as T
        File::class -> if (isNull(value)) null else parseFile(value) as T
        Set::class -> if (isNull(value)) null else parseSetOfStrings(value) as T
        Int::class -> if (isNull(value)) null else value.toInt() as T
        Boolean::class -> if (isNull(value)) null else value.toBoolean() as T
        else -> throw IllegalArgumentException("Unexpected type parameter ${T::class}")
    }
}