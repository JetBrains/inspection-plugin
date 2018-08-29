package org.jetbrains.idea.inspections.generators

import org.jetbrains.idea.inspections.problems.PinnedProblemDescriptor
import org.jetbrains.idea.inspections.problems.ProblemLevel
import java.io.File

interface ReportGenerator {
    val reportFile: File

    fun report(problem: PinnedProblemDescriptor, inspectionClass: String)

    fun generate()
}