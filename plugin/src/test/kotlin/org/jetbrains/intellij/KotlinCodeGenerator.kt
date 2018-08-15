package org.jetbrains.intellij

import java.io.File


val Int.kotlinCode: String
    get() = toString()

val Boolean.kotlinCode: String
    get() = toString()

val String.kotlinCode: String
    get() = replace("\\", "\\\\").replace("\"", "\\\"").let { """"$it"""" }

fun File.kotlinCode(base: String): String = File(base)
        .let { absoluteFile.relativeTo(it).path.kotlinCode }
        .let { "File($it)" }

val Set<String>.kotlinCode: String
    get() = joinToString(", ") { it.kotlinCode }.let { "setOf($it)" }
