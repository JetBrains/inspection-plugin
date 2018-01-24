package org.jetbrains.intellij

import org.gradle.api.logging.LogLevel

class InspectionClassesSuite private constructor(
        private val errors: Set<String>,
        private val warnings: Set<String>,
        private val infos: Set<String>,
        val inheritFromIdea: Boolean
) {
    val classes = errors + warnings + infos

    fun getLevel(clazz: String) = when (clazz) {
        in errors -> LogLevel.ERROR
        in warnings -> LogLevel.WARN
        in infos -> LogLevel.INFO
        else -> null
    }

    constructor(
            errors: List<String> = emptyList(),
            warnings: List<String> = emptyList(),
            infos: List<String> = emptyList()
    ): this(errors.toSet(), warnings.toSet(), infos.toSet(), inheritFromIdea = false)

    constructor(): this(emptySet(), emptySet(), emptySet(), inheritFromIdea = true)

    override fun toString() = "InheritFromIdea: $inheritFromIdea Errors: $errors Warnings: $warnings Info: $infos"
}