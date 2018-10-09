package org.jetbrains.intellij

import java.io.File

val File.allSourceFiles: Sequence<File>
    get() = walk().asSequence().filter { it.isSource }

val File.allExpectedSourceFiles: Sequence<File>
    get() = walk().asSequence().filter { it.isExpectedSource }

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

fun File.toMavenLayout(): File {
    return when {
        isKotlinSource -> File("src/main/kotlin", path)
        isJavaSource -> File("src/main/java", path)
        else -> throw IllegalArgumentException("Undefined language of source file $this")
    }
}
