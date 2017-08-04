package org.jetbrains.intellij

class InspectionPluginExtension(
        // Which properties are needed?
        // Number of IDEA version
        var ideaVersion: String,
        // List of necessary IDEA plugin
        var ideaPlugins: List<String>,
        // List of necessary inspections to run
        var inspectionClasses: List<String>
)