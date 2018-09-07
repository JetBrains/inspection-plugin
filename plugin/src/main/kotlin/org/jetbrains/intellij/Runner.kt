package org.jetbrains.intellij

import org.jetbrains.intellij.parameters.Plugin
import java.io.File
import java.util.function.BiFunction

interface Runner<in T : Runner.Parameters> {

    interface Parameters

    // Returns true if analysis executed successfully
    fun run(
            testMode: Boolean,
            files: Collection<File>,
            projectDir: File,
            projectName: String,
            moduleName: String,
            ideaVersion: String,
            ideaHomeDirectory: File,
            ideaSystemDirectory: File,
            plugins: List<Plugin>,
            parameters: T
    ): Boolean

    fun setLogger(logger: BiFunction<Int, String, Unit>)

    fun finalize()
}