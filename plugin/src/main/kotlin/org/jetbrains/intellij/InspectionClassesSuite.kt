package org.jetbrains.intellij

class InspectionClassesSuite private constructor(
        private val errors: Set<String>,
        private val warnings: Set<String>,
        private val infos: Set<String>,
        val inheritFromIdea: Boolean,
        val ideaProfile: String
) {
    val classes = errors + warnings + infos

    fun getLevel(clazz: String) = when (clazz) {
        in errors -> ProblemLevel.ERROR
        in warnings -> ProblemLevel.WARNING
        in infos -> ProblemLevel.INFORMATION
        else -> null
    }

    constructor(errors: List<String>, warnings: List<String>, infos: List<String>):
            this(errors.toSet(), warnings.toSet(), infos.toSet(), inheritFromIdea = false, ideaProfile = "")

    constructor(ideaProfile: String?):
            this(emptySet(), emptySet(), emptySet(), inheritFromIdea = true, ideaProfile = ideaProfile ?: DEFAULT_PROFILE)

    override fun toString() = "InheritFromIdea: $inheritFromIdea Errors: $errors Warnings: $warnings Info: $infos"

    companion object {
        private const val DEFAULT_PROFILE = "Project_Default.xml"
    }
}