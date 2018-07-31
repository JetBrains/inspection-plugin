package org.jetbrains.intellij.parameters

import java.io.File

data class ReportParameters(
        val isQuiet: Boolean,
        val xml: File?,
        val html: File?
)