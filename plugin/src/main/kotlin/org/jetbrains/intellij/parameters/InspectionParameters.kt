package org.jetbrains.intellij.parameters

import java.io.Serializable

data class InspectionParameters(
        val name: String,
        val quickFix: Boolean
) : Serializable
