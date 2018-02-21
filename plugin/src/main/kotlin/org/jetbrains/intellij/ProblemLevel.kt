package org.jetbrains.intellij

import org.gradle.api.logging.LogLevel

enum class ProblemLevel(val logLevel: LogLevel) {
    ERROR(LogLevel.ERROR),
    WARNING(LogLevel.WARN),
    WEAK_WARNING(LogLevel.WARN),
    INFORMATION(LogLevel.INFO);

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