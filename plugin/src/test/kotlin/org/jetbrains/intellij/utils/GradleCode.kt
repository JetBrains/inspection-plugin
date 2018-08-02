package org.jetbrains.intellij.utils

import java.io.File


val Int.gradleCode: String
    get() = toString()

val Boolean.gradleCode: String
    get() = toString()

val String.gradleCode: String
    get() = "'${replace("\\", "\\\\").replace("\'","\\\'")}'"

val File.gradleCode: String
    get() = "file(${absolutePath.gradleCode})"

val Set<String>.gradleCode: String
    get() = "[${joinToString(", ") { it.gradleCode }}]"