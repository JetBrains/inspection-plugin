package org.jetbrains.idea.inspections.generators

import org.jetbrains.idea.inspections.PinnedProblemDescriptor
import org.jetbrains.idea.inspections.ProblemLevel
import java.io.File

interface ReportGenerator {
    val reportFile: File

    fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String)

    fun generate()
}