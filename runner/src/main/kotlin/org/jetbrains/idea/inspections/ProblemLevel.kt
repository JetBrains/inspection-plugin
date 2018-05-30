package org.jetbrains.idea.inspections

enum class ProblemLevel {
    ERROR,
    WARNING,
    WEAK_WARNING,
    INFORMATION;

    companion object {
        fun fromInspectionEPLevel(level: String): ProblemLevel? = when (level) {
            "ERROR" -> ERROR
            "WARNING" -> WARNING
            "WEAK_WARNING" -> WEAK_WARNING
            "INFO" -> INFORMATION
            "INFORMATION" -> null
            else -> WEAK_WARNING
        }
    }
}