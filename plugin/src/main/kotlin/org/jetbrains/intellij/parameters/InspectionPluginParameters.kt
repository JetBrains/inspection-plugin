package org.jetbrains.intellij.parameters

import org.jetbrains.intellij.Runner


data class InspectionPluginParameters(
        val testMode: Boolean,
        val ignoreFailures: Boolean,
        val ideaVersion: String,
        val kotlinPluginVersion: String?,

        val isAvailableCodeChanging: Boolean,
        val reportParameters: ReportParameters,

        val inheritFromIdea: Boolean,
        val profileName: String?,
        val errors: InspectionsParameters,
        val warnings: InspectionsParameters,
        val infos: InspectionsParameters
) : Runner.Parameters {

    val inspections = errors.inspections + warnings.inspections + infos.inspections

}
