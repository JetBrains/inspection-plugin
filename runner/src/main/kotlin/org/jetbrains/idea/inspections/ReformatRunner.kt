package org.jetbrains.idea.inspections

import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.openapi.project.Project
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters

@Suppress("unused")
class ReformatRunner : FileInfoRunner<InspectionsRunnerParameters>() {
    override fun analyze(
            files: Collection<FileInfo>,
            project: Project,
            parameters: InspectionsRunnerParameters
    ): Boolean {
        val codeStyleManager = CodeStyleManager.getInstance(project)
        for ((psiFile, document) in files) {
            runReadAction {
                val fileName = psiFile.name
                val beforeText = psiFile.text
                codeStyleManager.reformat(psiFile)
                val afterText = psiFile.text
                when (afterText) {
                    beforeText -> logger.info("InspectionPlugin: File $fileName hasn't changes after fix.")
                    else -> logger.info("InspectionPlugin: File $fileName has changes after fix.")
                }
            }
        }
        return true
    }
}