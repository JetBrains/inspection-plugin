package org.jetbrains.intellij.tasks

import org.jetbrains.intellij.BaseType

interface SourceBaseDependentTask {

    /**
     * Type of analyzable source set
     */
    var baseType: BaseType
}