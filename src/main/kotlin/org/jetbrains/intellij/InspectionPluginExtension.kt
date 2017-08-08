package org.jetbrains.intellij

import org.gradle.api.logging.LogLevel

open class InspectionPluginExtension(
        // Which properties are needed?
        // Number of IDEA version
        var ideaVersion: String,
        // List of necessary IDEA plugin
        var ideaPlugins: List<String>,
        // List of necessary inspections to run
        var errorClasses: List<String>,
        var warningClasses: List<String>,
        var infoClasses: List<String>
) {
    constructor(): this("", emptyList(), emptyList(), emptyList(), emptyList())

    val inspectionClasses get() = errorClasses + warningClasses + infoClasses

    fun getLevel(inspectionClass: String) = when (inspectionClass) {
        in errorClasses -> LogLevel.ERROR
        in warningClasses -> LogLevel.WARN
        else -> LogLevel.INFO
    }
}