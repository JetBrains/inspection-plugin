package org.jetbrains.idea.inspections

import com.intellij.history.core.Paths
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

fun PsiElement.acceptRecursively(visitor: PsiElementVisitor) {
    this.accept(visitor)
    for (child in this.children) {
        child.acceptRecursively(visitor)
    }
}

fun PsiElement.getLine(document: Document?): Int {
    return document?.getLineNumber(textRange.startOffset) ?: -1
}

fun PsiElement.getRow(document: Document, line: Int = getLine(document)): Int {
    return textRange.startOffset - document.getLineStartOffset(line)
}

val PsiElement.relativeFilePath: String
    get() {
        val basePath = project.basePath ?: ""
        val ourPath = containingFile.virtualFile.canonicalPath ?: ""
        return Paths.relativeIfUnder(ourPath, basePath)
    }
