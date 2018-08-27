package org.jetbrains.idea.inspections

import org.jetbrains.intellij.Runner
import org.jetbrains.intellij.Logger
import java.util.function.BiFunction

abstract class AbstractRunner<T> : Runner<T> {
    protected var logger = Logger(BiFunction { t, u -> })
        private set

    override fun setLogger(logger: BiFunction<Int, String, Unit>) {
        this.logger = Logger(logger)
    }
}