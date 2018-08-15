package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.Internal
import org.jetbrains.intellij.SourceSetType

interface SourceBasedTask {
    /**
     * Type of analyzable source set
     */
    @get:Internal
    var sourceSetType: SourceSetType
}