package org.jetbrains.intellij.utils

import org.gradle.api.file.FileTree
import java.io.File

interface Unpack {
    operator fun invoke(archive: File): FileTree
}