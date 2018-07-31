package org.jetbrains.idea.inspections

import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.editor.Document as IdeaDocument

@Suppress("unused")
class InspectionsRunner(testMode: Boolean, inspections: Set<String>) : AbstractInspectionsRunner(testMode, inspections)
