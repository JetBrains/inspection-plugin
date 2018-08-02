package org.jetbrains.intellij.utils

import java.io.File

val File.allSourceFiles: Sequence<File>
    get() = walk().asSequence().filter { it.isSource }

val File.singleSourceFile: File?
    get() = listFiles()?.find { it.isSource }

val File.allExpectedSourceFiles: Sequence<File>
    get() = walk().asSequence().filter { it.isExpectedSource }

val File.singleExpectedSourceFile: File?
    get() = listFiles()?.find { it.isExpectedSource }

val File.isSource: Boolean
    get() = isKotlinSource || isJavaSource

val File.isExpectedSource: Boolean
    get() = isKotlinExpectedSource || isJavaExpectedSource

val File.isJavaSource: Boolean
    get() = extension == "java"

val File.isKotlinSource: Boolean
    get() = extension == "kt"

val File.isJavaExpectedSource: Boolean
    get() = extension == "javax"

val File.isKotlinExpectedSource: Boolean
    get() = extension == "ktx"

fun File.isSourceFor(expected: File) =
        nameWithoutExtension == expected.nameWithoutExtension && parentFile.path == expected.parentFile.path
