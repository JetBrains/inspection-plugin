package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.*
import org.jetbrains.intellij.parameters.InspectionParameters


@CacheableTask
open class ReformatTask : AbstractInspectionsTask() {

    override val errorsInspections: Map<String, InspectionParameters>
        get() = emptyMap()

    override val maxErrors: Int?
        get() = null

    override val warningsInspections: Map<String, InspectionParameters>
        get() = mapOf(reformatInspectionToolParameters.name to reformatInspectionToolParameters)

    override val maxWarnings: Int?
        get() = null

    override val infosInspections: Map<String, InspectionParameters>
        get() = emptyMap()

    override val maxInfos: Int?
        get() = null

    override val inheritFromIdea: Boolean
        get() = false

    override val profileName: String?
        get() = null

    @get:Internal
    private val reformatQuickFix: Boolean
        get() = extension.reformat.quickFix ?: true

    @get:Internal
    private val reformatInspectionToolParameters: InspectionParameters
        get() = InspectionParameters(REFORMAT_INSPECTION_TOOL, reformatQuickFix)

    companion object {
        private const val REFORMAT_INSPECTION_TOOL = "org.jetbrains.kotlin.idea.inspections.ReformatInspection"
    }
}