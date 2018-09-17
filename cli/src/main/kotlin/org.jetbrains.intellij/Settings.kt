package org.jetbrains.intellij

import java.io.File

data class Settings(
        val runner: File?,
        val idea: File?,
        val inheritFromIdea: Boolean?,
        val profileName: String?,
        val report: Report?,
        val reformat: Inspection?,
        val errors: Inspections?,
        val warnings: Inspections?,
        val info: Inspections?
) {
    private fun <K, V> Map<K, V>?.toMap(): Map<K, V> = this ?: emptyMap()

    val inspections = errors?.inspections.toMap() + warnings?.inspections.toMap() + info?.inspections.toMap()

    data class Report(val isQuiet: Boolean?, val html: File?, val xml: File?)

    data class Inspection(val quickFix: Boolean?)

    data class Inspections(val inspections: Map<String, Inspection>, val max: Int?)

    companion object {
        val EMPTY = Settings(null, null, null, null, null, null, null, null, null)
    }
}