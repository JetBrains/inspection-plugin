package org.jetbrains.intellij

import java.io.File

data class SettingsBuilder(
        var runner: File? = null,
        var idea: File? = null,
        var ignoreFailures: Boolean? = null,
        var inheritFromIdea: Boolean? = null,
        var profileName: String? = null,
        val report: Report = Report(),
        val reformat: Inspection = Inspection(),
        val errors: Inspections = Inspections(),
        val warnings: Inspections = Inspections(),
        val info: Inspections = Inspections()
) {
    val inspections = errors.inspections + warnings.inspections + info.inspections

    data class Report(var isQuiet: Boolean? = null, var html: File? = null, var xml: File? = null)

    data class Inspection(var quickFix: Boolean? = null)

    data class Inspections(val inspections: MutableMap<String, Inspection> = HashMap(), var max: Int? = null)
}