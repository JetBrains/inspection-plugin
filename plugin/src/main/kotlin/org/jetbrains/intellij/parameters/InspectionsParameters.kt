package org.jetbrains.intellij.parameters

import org.jetbrains.intellij.Runner
import java.io.File


data class InspectionsParameters(
        val ignoreFailures: Boolean,
        val ideaVersion: String,
        val kotlinPluginVersion: String?,

        val projectDir: File,
        val reportParameters: ReportParameters,
        val quickFix: Boolean,
        val skipBinarySources: Boolean,

        val inheritFromIdea: Boolean,
        val profileName: String?,
        val errors: InspectionTypeParameters,
        val warnings: InspectionTypeParameters,
        val infos: InspectionTypeParameters
) : Runner.Parameters
