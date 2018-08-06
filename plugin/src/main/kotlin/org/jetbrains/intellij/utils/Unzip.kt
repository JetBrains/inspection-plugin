package org.jetbrains.intellij.utils

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import java.io.File

class Unzip(private val project: Project): Unpack {
    override operator fun invoke(archive: File): FileTree = project.zipTree(archive)
}