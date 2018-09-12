package org.jetbrains.idea.inspections.runners

import org.jetbrains.intellij.ProxyLogger

abstract class Runner<in T>(protected val logger: ProxyLogger) {
    abstract fun run(parameters: T): Boolean

    abstract fun finalize()
}
