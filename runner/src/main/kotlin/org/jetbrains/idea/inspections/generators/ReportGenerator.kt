package org.jetbrains.idea.inspections.generators

import org.jetbrains.idea.inspections.problems.DisplayableProblemDescriptor
import java.io.File

interface ReportGenerator {
    val reportFile: File

    fun report(problem: DisplayableProblemDescriptor<*>, inspectionClass: String)

    fun generate()
}