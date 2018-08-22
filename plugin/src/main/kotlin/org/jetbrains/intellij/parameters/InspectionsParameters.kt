package org.jetbrains.intellij.parameters

data class InspectionsParameters(
        val inspections: Map<String, InspectionParameters>,
        val max: Int?
)
