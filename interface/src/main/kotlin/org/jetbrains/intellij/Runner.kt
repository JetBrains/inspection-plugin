package org.jetbrains.intellij

import java.util.function.BiFunction

interface Runner<in T> {

    // Returns true if analysis executed successfully
    fun run(parameters: T): Boolean

    fun setLogger(logger: BiFunction<Int, String, Unit>)

    fun finalize()
}