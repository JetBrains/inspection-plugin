package org.jetbrains.intellij.tasks

import org.gradle.api.file.FileTree


abstract class SourceTask : org.gradle.api.tasks.SourceTask() {
    /**
     * This method needed for save backward compatibility with gradle 3.8 and lower
     */
    fun setSourceSet(source: FileTree) = setSource(source as Any)
}