package org.jetbrains.intellij

import java.io.File


val Int.kotlinCode: String
    get() = toString()

val Boolean.kotlinCode: String
    get() = toString()

val String.kotlinCode: String
    get() = replace("\\", "/").replace("\"", "\\\"").let { """"$it"""" }

val Set<String>.kotlinCode: String
    get() = joinToString(", ") { it.kotlinCode }.let { "setOf($it)" }

val LoggerLevel.kotlinCode: String
    get() = "LoggerLevel.$this"

val List<String>.kotlinCode: String
    get() = joinToString(", ") { it.kotlinCode }.let { "listOf($it)" }

val File.kotlinCode: String
    get() = "File(${path.kotlinCode})"

fun File.kotlinCode(base: File): String = "File(${absoluteFile.relativeTo(base).path.kotlinCode})"
