package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.*


@CacheableTask
open class ReformatTask : AbstractInspectionsTask() {

    override val errorsInspections: Set<String>
        get() = emptySet()

    override val maxErrors: Int?
        get() = null

    override val warningsInspections: Set<String>
        get() = setOf(REFORMAT_INSPECTION_TOOL)

    override val maxWarnings: Int?
        get() = null

    override val infosInspections: Set<String>
        get() = emptySet()

    override val maxInfos: Int?
        get() = null

    override val isQuiet: Boolean
        get() = extension.reformat.isQuiet ?: false

    override val quickFix: Boolean
        get() = extension.reformat.quickFix ?: true

    override val inheritFromIdea: Boolean
        get() = false

    override val profileName: String?
        get() = null

    companion object {
        private const val REFORMAT_INSPECTION_TOOL = "org.jetbrains.kotlin.idea.inspections.ReformatInspection"
    }
}