package org.jetbrains.intellij.parameters

import java.io.File
import java.io.Serializable

data class IdeaRunnerParameters<T>(
        val projectDir: File,
        val projectName: String,
        val moduleName: String,
        val ideaHomeDirectory: File,
        val ideaSystemDirectory: File,
        val plugins: List<File>,
        val childParameters: T
) : Serializable