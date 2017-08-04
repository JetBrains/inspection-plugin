package org.jetbrains.intellij

import org.gradle.api.logging.LogLevel

class InspectionPluginExtension(
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
    val inspectionClasses get() = errorClasses + warningClasses + infoClasses

    fun getLevel(inspectionClass: String) = when {
        inspectionClass in errorClasses -> LogLevel.ERROR
        inspectionClass in warningClasses -> LogLevel.WARN
        else -> LogLevel.INFO
    }
}