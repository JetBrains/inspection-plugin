package org.jetbrains.idea.inspections.runners

import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.openapi.project.Project
import org.jetbrains.intellij.Logger
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters

@Suppress("unused")
class ReformatRunner(logger: Logger) : FileInfoRunner<InspectionsRunnerParameters>(logger) {
    override fun analyze(
            files: Collection<FileInfo>,
            project: Project,
            parameters: InspectionsRunnerParameters
    ): Boolean {
        val codeStyleManager = CodeStyleManager.getInstance(project)
        @Suppress("UNUSED_VARIABLE")
        for ((psiFile, document) in files) {
            runReadAction {
                val fileName = psiFile.name
                val beforeText = psiFile.text
                codeStyleManager.reformat(psiFile)
                val afterText = psiFile.text
                when (afterText) {
                    beforeText -> logger.info("File $fileName hasn't changes after fix.")
                    else -> logger.info("File $fileName has changes after fix.")
                }
            }
        }
        return true
    }
}