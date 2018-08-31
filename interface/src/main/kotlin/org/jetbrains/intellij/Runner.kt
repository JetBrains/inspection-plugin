package org.jetbrains.intellij

interface Runner<in T> {

    // Returns true if analysis executed successfully
    fun run(parameters: T): Boolean

    fun finalize()
}