package org.jetbrains.intellij

import java.io.File
import java.util.function.BiFunction

interface Analyzer {
    // Returns true if analysis executed successfully
    fun analyzeTreeAndLogResults(
            files: Collection<File>,
            ideaProjectFileName: String,
            ideaHomeDirectory: File,
            maxErrors: Int,
            maxWarnings: Int,
            quiet: Boolean,
            xmlReport: File?,
            htmlReport: File?
    ): Boolean

    fun setLogger(logger: BiFunction<Int, String, Unit>)

    fun shutdownIdea()
}