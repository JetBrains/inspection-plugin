package org.jetbrains.idea.inspections

import org.gradle.api.reporting.SingleFileReport
import org.jetbrains.intellij.ProblemLevel

class HTMLGenerator(override val report: SingleFileReport) : ReportGenerator {
    private val sb = StringBuilder()

    override fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String) {

    }

    override fun generate() {
        val htmlReportFile = report.destination
        htmlReportFile.writeText(sb.toString())
    }
}