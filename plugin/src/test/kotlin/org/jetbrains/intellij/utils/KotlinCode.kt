package org.jetbrains.intellij.utils

import java.io.File


val Int.kotlinCode: String
    get() = toString()

val Boolean.kotlinCode: String
    get() = toString()

val String.kotlinCode: String
    get() = "\"${replace("\\", "\\\\").replace("\"","\\\"")}\""

val File.kotlinCode: String
    get() = "File(${absolutePath.kotlinCode})"

val Set<String>.kotlinCode: String
    get() = "setOf(${joinToString(", ") { it.kotlinCode }})"