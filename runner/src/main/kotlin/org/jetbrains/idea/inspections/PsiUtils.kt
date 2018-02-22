package org.jetbrains.idea.inspections

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

fun PsiElement.acceptRecursively(visitor: PsiElementVisitor) {
    this.accept(visitor)
    for (child in this.children) {
        child.acceptRecursively(visitor)
    }
}

fun PsiElement.getLine(document: Document): Int {
    return document.getLineNumber(textRange.startOffset)
}

fun PsiElement.getRow(document: Document, line: Int = getLine(document)): Int {
    return textRange.startOffset - document.getLineStartOffset(line)
}