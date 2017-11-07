package org.jetbrains.intellij

import org.gradle.api.logging.LogLevel

class InspectionClassesSuite(val errors: Set<String>, val warnings: Set<String>, val infos: Set<String>) {
    val classes = errors + warnings + infos

    fun getLevel(clazz: String) = when (clazz) {
        in errors -> LogLevel.ERROR
        in warnings -> LogLevel.WARN
        else -> LogLevel.INFO
    }

    constructor(errors: List<String>, warnings: List<String>, infos: List<String>):
            this(errors.toSet(), warnings.toSet(), infos.toSet())

    override fun toString() = "Errors: $errors Warnings: $warnings Info: $infos"
}