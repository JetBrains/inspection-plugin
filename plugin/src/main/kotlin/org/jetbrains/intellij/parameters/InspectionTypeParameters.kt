package org.jetbrains.intellij.parameters

data class InspectionTypeParameters(
        val inspections: Set<String>,
        val max: Int?
)
