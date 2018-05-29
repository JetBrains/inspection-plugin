package org.jetbrains.idea.inspections

import org.jetbrains.intellij.ProblemLevel
import java.io.File

interface ReportGenerator {
    val reportFile: File

    fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String)

    fun generate()
}