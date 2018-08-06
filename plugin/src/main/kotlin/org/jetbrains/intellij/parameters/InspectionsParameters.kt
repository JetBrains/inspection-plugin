package org.jetbrains.intellij.parameters

import org.jetbrains.intellij.Analyzer
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import java.io.File


data class InspectionsParameters(
        val ignoreFailures: Boolean,
        val ideaVersion: IdeaVersion,
        val kotlinPluginVersion: KotlinPluginVersion?,

        val projectDir: File,
        val report: ReportParameters,
        val quickFix: Boolean,
        val skipBinarySources: Boolean,

        val inheritFromIdea: Boolean,
        val profileName: String?,
        val errors: InspectionTypeParameters,
        val warnings: InspectionTypeParameters,
        val infos: InspectionTypeParameters
) : Analyzer.Parameters
