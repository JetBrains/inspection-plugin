package org.jetbrains.intellij.parameters

import org.jetbrains.intellij.plugins.Plugin
import java.io.File
import java.io.Serializable

data class IdeaRunnerParameters<T>(
        val projectDir: File,
        val projectName: String,
        val moduleName: String,
        val ideaVersion: String,
        val ideaHomeDirectory: File,
        val ideaSystemDirectory: File,
        val plugins: List<Plugin>,
        val childParameters: T
) : Serializable