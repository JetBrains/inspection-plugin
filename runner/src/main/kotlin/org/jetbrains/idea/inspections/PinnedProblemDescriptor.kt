package org.jetbrains.idea.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorUtil
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.text.StringUtil

class PinnedProblemDescriptor(
        val descriptor: ProblemDescriptor,
        val fileName: String,
        val line: Int,
        val row: Int,
        val displayName: String?,
        val ideaLevel: ProblemLevel?
) : ProblemDescriptor by descriptor {
    private val highlightedText = psiElement?.let {
        ProblemDescriptorUtil.extractHighlightedText(this, it)
    } ?: ""

    fun renderLocation(): String = StringBuilder().apply {
        append(fileName)
        append(":")
        append(line + 1)
        append(":")
        append(row + 1)
    }.toString()

    fun renderWithLocation(): String = renderLocation() + ": " + render()

    fun render(): String = StringUtil.replace(
            StringUtil.replace(
                    descriptionTemplate,
                    "#ref",
                    highlightedText
            ),
            " #loc",
            ""
    )

    constructor(descriptor: ProblemDescriptor, document: Document, displayName: String?,
                problemLevel: ProblemLevel?,
                lineNumber: Int = descriptor.psiElement.getLine(document)):
            this(descriptor, descriptor.psiElement.containingFile.name, lineNumber,
                 descriptor.psiElement.getRow(document, lineNumber),
                 displayName, problemLevel)

    fun actualLevel(default: ProblemLevel?): ProblemLevel? = when (highlightType) {
        // Default (use level either from IDEA configuration or plugin configuration)
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
        ProblemHighlightType.LIKE_DEPRECATED,
        ProblemHighlightType.LIKE_UNUSED_SYMBOL ->
            default ?: this.ideaLevel
        // If inspection forces error, report it
        ProblemHighlightType.ERROR, ProblemHighlightType.GENERIC_ERROR ->
            ProblemLevel.ERROR
        // If inspection forces weak warning, never report error
        ProblemHighlightType.WEAK_WARNING ->
            if (default == ProblemLevel.ERROR || default == null) ProblemLevel.WEAK_WARNING else default
        // If inspection forces "do not show", it's really not a problem at all
        ProblemHighlightType.INFORMATION ->
            null
        else /* INFO only */ ->
            ProblemLevel.INFORMATION
    }
}