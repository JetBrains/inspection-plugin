package org.jetbrains.intellij

import java.io.File


val Int.gradleCode: String
    get() = toString()

val Boolean.gradleCode: String
    get() = toString()

val String.gradleCode: String
    get() = "'${replace("\\", "\\\\").replace("\'", "\\\'")}'"

@Suppress("unused")
val File.gradleCode: String
    get() = "file(${absolutePath.gradleCode})"

@Suppress("unused")
val Set<String>.gradleCode: String
    get() = "[${joinToString(", ") { it.gradleCode }}]"
