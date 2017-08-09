package org.jetbrains.intellij

import org.gradle.api.logging.LogLevel

open class InspectionPluginExtension(
        // Which properties are needed?
        // Number of IDEA version
        open var ideaVersion: String,
        // List of necessary IDEA plugin
        open var ideaPlugins: Array<String>,
        // List of necessary inspections to run
        open var errorClasses: Array<String>,
        open var warningClasses: Array<String>,
        open var infoClasses: Array<String>
) {
    constructor(): this("ideaIC:2017.2", emptyArray(), emptyArray(), emptyArray(), emptyArray())

    val inspectionClasses get() = errorClasses + warningClasses + infoClasses

    fun getLevel(inspectionClass: String) = when (inspectionClass) {
        in errorClasses -> LogLevel.ERROR
        in warningClasses -> LogLevel.WARN
        else -> LogLevel.INFO
    }
}