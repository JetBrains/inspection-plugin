package org.jetbrains.intellij

class InspectionClassesSuite private constructor(
        private val errors: Set<String>,
        private val warnings: Set<String>,
        private val infos: Set<String>,
        val inheritFromIdea: Boolean
) {
    val classes = errors + warnings + infos

    fun getLevel(clazz: String) = when (clazz) {
        in errors -> ProblemLevel.ERROR
        in warnings -> ProblemLevel.WARNING
        in infos -> ProblemLevel.INFORMATION
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