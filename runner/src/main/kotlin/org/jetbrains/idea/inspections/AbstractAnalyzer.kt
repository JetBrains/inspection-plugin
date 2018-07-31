package org.jetbrains.idea.inspections

import org.jetbrains.intellij.Analyzer
import org.jetbrains.intellij.Logger
import java.util.function.BiFunction

abstract class AbstractAnalyzer<T: Analyzer.Parameters> : Analyzer<T> {
    protected var logger = Logger(BiFunction { t, u -> })
        private set

    override fun setLogger(logger: BiFunction<Int, String, Unit>) {
        this.logger = Logger(logger)
    }
}