package org.jetbrains.idea.inspections.problems

import com.intellij.psi.PsiElement

sealed class Entity<out T>(val reference: T) {
    class Element(element: PsiElement): Entity<PsiElement>(element)
}