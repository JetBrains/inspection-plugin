package org.jetbrains.intellij

import java.io.File
import java.util.function.BiFunction

interface Runner<in T : Runner.Parameters> {

    interface Parameters

    // Returns true if analysis executed successfully
    fun run(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            ideaHomeDirectory: File,
            plugins: List<File>,
            parameters: T
    ): Boolean

    fun setLogger(logger: BiFunction<Int, String, Unit>)

    fun finalize()
}