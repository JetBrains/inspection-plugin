package org.jetbrains.intellij

import org.gradle.api.file.FileTree

interface Analyzer {
    // Returns true if analysis executed successfully
    fun analyzeTreeAndLogResults(tree: FileTree): Boolean
}