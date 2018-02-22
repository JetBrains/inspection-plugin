package org.jetbrains.idea.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.intellij.ProblemLevel

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
                lineNumber: Int = document.getLineNumber(descriptor.psiElement.textRange.startOffset)):
            this(descriptor, descriptor.psiElement.containingFile.name, lineNumber,
                 descriptor.psiElement.textRange.startOffset - document.getLineStartOffset(lineNumber),
                 displayName, problemLevel)
}