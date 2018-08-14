package org.jetbrains.intellij

import java.io.File
import java.util.function.BiFunction

interface Analyzer<T : Analyzer.Parameters> {

    interface Parameters

    // Returns true if analysis executed successfully
    fun analyze(
            files: Collection<File>,
            projectName: String,
            moduleName: String,
            ideaHomeDirectory: File,
            parameters: T
    ): Boolean

    fun setLogger(logger: BiFunction<Int, String, Unit>)

    fun finalize()
}