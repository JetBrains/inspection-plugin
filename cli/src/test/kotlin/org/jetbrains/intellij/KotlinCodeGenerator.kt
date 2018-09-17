package org.jetbrains.intellij

import java.io.File

val LoggerLevel.kotlinCode: String
    get() = "LoggerLevel.$this"

val List<String>.kotlinCode: String
    get() = joinToString(", ") { it.kotlinCode }.let { "listOf($it)" }

val String.kotlinCode: String
    get() = replace("\\", "/").replace("\"", "\\\"").let { """"$it"""" }

fun File.kotlinCode(base: File): String = "File(${absoluteFile.relativeTo(base).path.kotlinCode})"
