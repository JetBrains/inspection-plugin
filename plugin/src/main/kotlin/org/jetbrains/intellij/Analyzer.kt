package org.jetbrains.intellij

import java.io.File

interface Analyzer {
    // Returns true if analysis executed successfully
    fun analyzeTreeAndLogResults(files: Collection<File>, ideaHomeDirectory: File): Boolean
}