package org.jetbrains.idea.inspections

import org.gradle.api.reporting.SingleFileReport
import org.jetbrains.intellij.ProblemLevel

interface ReportGenerator {
    val report: SingleFileReport

    val enabled: Boolean get() = report.isEnabled

    fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String)

    fun generate()
}