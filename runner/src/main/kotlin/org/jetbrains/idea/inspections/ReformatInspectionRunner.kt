package org.jetbrains.idea.inspections

import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.editor.Document as IdeaDocument
import org.jetbrains.kotlin.idea.inspections.ReformatInspection

@Suppress("unused")
class ReformatInspectionRunner(projectPath: String, testMode: Boolean) :
        AbstractInspectionsRunner(projectPath, testMode, false, setOf(REFORMAT_INSPECTION_TOOL), null) {

    companion object {
        private val REFORMAT_INSPECTION_TOOL = ReformatInspection::class.java.canonicalName
    }
}