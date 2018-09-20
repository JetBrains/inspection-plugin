package org.jetbrains.idea.inspections.problems

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorUtil
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Document
import org.jetbrains.idea.inspections.getLine
import org.jetbrains.idea.inspections.getRow
import org.jetbrains.idea.inspections.relativeFilePath
import com.intellij.openapi.util.text.StringUtil.replace
import com.intellij.psi.PsiElement
import org.jetbrains.idea.inspections.getLine
import org.jetbrains.idea.inspections.getRow

class PinnedProblemDescriptor private constructor(
        private val descriptor: ProblemDescriptor,
        val fileName: String,
        val line: Int,
        val row: Int,
        override val displayName: String,
        override val level: ProblemLevel
) : ProblemDescriptor by descriptor, DisplayableProblemDescriptor<PsiElement> {

    override val entity = psiElement?.let { Entity.Element(it) }

    private val highlightedText = psiElement?.let {
        ProblemDescriptorUtil.extractHighlightedText(this, it)
    } ?: ""

    fun renderLocation() = "$fileName:${line + 1}:${row + 1}"

    private fun String.replace(oldS: String, newS: String) = replace(this, oldS, newS)

    fun renderDescription() = descriptionTemplate.replace("#ref", highlightedText).replace(" #loc", "")

    override fun render() = renderLocation() + ": " + renderDescription()

    companion object {
        fun createIfProblem(
                descriptor: ProblemDescriptor,
                document: Document,
                displayName: String,
                problemLevel: ProblemLevel?
        ): PinnedProblemDescriptor? {
            val level = actualLevel(problemLevel, descriptor.highlightType)
                    ?: return null
            val lineNumber = descriptor.psiElement.getLine(document)
            val fileName = descriptor.psiElement.relativeFilePath
            val rowNumber = descriptor.psiElement.getRow(document, lineNumber)
            return PinnedProblemDescriptor(descriptor, fileName, lineNumber, rowNumber, displayName, level)
        }

        private fun actualLevel(ideaLevel: ProblemLevel?, highlightType: ProblemHighlightType): ProblemLevel? {
            return when (highlightType) {
                // Default (use level either from IDEA configuration or plugin configuration)
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING -> ideaLevel
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL -> ideaLevel
                ProblemHighlightType.LIKE_DEPRECATED -> ideaLevel
                ProblemHighlightType.LIKE_UNUSED_SYMBOL -> ideaLevel
                // If inspection forces error, report it
                ProblemHighlightType.ERROR -> ProblemLevel.ERROR
                ProblemHighlightType.GENERIC_ERROR -> ProblemLevel.ERROR
                // If inspection forces weak warning, never report error
                ProblemHighlightType.WEAK_WARNING -> when (ideaLevel) {
                    ProblemLevel.ERROR -> ProblemLevel.WEAK_WARNING
                    else -> ideaLevel
                }
                // If inspection forces "do not show", it's really not a problem at all
                ProblemHighlightType.INFORMATION -> null
                else -> ProblemLevel.INFO
            }
        }
    }
}