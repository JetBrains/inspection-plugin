package org.jetbrains.idea.inspections

import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.editor.Document as IdeaDocument

@Suppress("unused")
class InspectionsRunner(
        projectPath: String,
        inheritFromIdea: Boolean,
        inspections: Set<String>?,
        ideaProfile: String?,
        testMode: Boolean
) : AbstractInspectionsRunner(
        projectPath,
        testMode,
        inheritFromIdea,
        inspections,
        ideaProfile
)
