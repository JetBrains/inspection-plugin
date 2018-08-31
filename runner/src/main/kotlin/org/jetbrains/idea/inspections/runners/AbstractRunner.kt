package org.jetbrains.idea.inspections.runners

import org.jetbrains.intellij.Logger
import org.jetbrains.intellij.Runner

abstract class AbstractRunner<T> : Runner<T> {
    protected val logger = Logger()
}