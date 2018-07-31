package org.jetbrains.intellij

data class AnalyzerParameters(
        val reportParameters: ReportParameters?,
        val quickFixParameters: QuickFixParameters?
)