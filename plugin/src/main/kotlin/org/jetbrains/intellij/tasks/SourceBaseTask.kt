package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.Internal
import org.jetbrains.intellij.BaseType

interface SourceBaseTask {
    /**
     * Type of analyzable source set
     */
    @get:Internal
    var baseType: BaseType
}