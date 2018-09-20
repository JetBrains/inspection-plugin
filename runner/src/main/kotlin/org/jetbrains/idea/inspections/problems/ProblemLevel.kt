package org.jetbrains.idea.inspections.problems

enum class ProblemLevel {
    ERROR,
    WARNING,
    WEAK_WARNING,
    INFO;

    companion object {
        fun fromInspectionEPLevel(level: String): ProblemLevel? = when (level) {
            "ERROR" -> ERROR
            "WARNING" -> WARNING
            "WEAK_WARNING" -> WEAK_WARNING
            "INFO" -> INFO
            "INFORMATION" -> null
            else -> WEAK_WARNING
        }
    }
}