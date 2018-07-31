package org.jetbrains.idea.inspections

import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.editor.Document as IdeaDocument
import org.jetbrains.kotlin.idea.inspections.ReformatInspection

@Suppress("unused")
class ReformatInspectionRunner(testMode: Boolean) : AbstractInspectionsRunner(testMode, setOf(REFORMAT_INSPECTION_TOOL)) {
    companion object {
        private val REFORMAT_INSPECTION_TOOL = ReformatInspection::class.java.canonicalName
    }
}