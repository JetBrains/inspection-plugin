package org.jetbrains.intellij

import java.io.File

data class ReportParameters(
        val errors: Set<String>,
        val warnings: Set<String>,
        val infos: Set<String>,
        val maxErrors: Int,
        val maxWarnings: Int,
        val quiet: Boolean,
        val xmlReport: File?,
        val htmlReport: File?
)