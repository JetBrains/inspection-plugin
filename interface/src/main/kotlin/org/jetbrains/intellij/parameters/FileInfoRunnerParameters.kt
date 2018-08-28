package org.jetbrains.intellij.parameters

import java.io.File
import java.io.Serializable

data class FileInfoRunnerParameters<T>(
        val files: Collection<File>,
        val childParameters: T
) : Serializable
