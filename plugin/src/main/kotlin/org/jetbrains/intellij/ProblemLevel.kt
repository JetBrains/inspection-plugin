package org.jetbrains.intellij

import org.gradle.api.logging.LogLevel

enum class ProblemLevel(val logLevel: LogLevel) {
    ERROR(LogLevel.ERROR),
    WARNING(LogLevel.WARN),
    WEAK_WARNING(LogLevel.WARN),
    INFORMATION(LogLevel.INFO)
}