package org.jetbrains.intellij.parameters

import java.io.File
import java.io.Serializable


data class InspectionsRunnerParameters(
        val ideaVersion: String,
        val kotlinPluginVersion: String?,

        val isAvailableCodeChanging: Boolean,
        val reportParameters: Report,

        val inheritFromIdea: Boolean,
        val profileName: String?,
        val errors: Inspections,
        val warnings: Inspections,
        val infos: Inspections
) : Serializable {
    val inspections = errors.inspections + warnings.inspections + infos.inspections

    data class Inspection(
            val name: String,
            val quickFix: Boolean
    ) : Serializable

    data class Inspections(
            val inspections: Map<String, Inspection>,
            val max: Int?
    ) : Serializable

    data class Report(
            val isQuiet: Boolean,
            val xml: File?,
            val html: File?
    ) : Serializable
}
