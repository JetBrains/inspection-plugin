package org.jetbrains.idea.inspections.problems

import com.intellij.codeInspection.reference.RefDirectory
import com.intellij.codeInspection.reference.RefEntity
import com.intellij.codeInspection.reference.RefFile
import com.intellij.codeInspection.reference.RefModule
import com.intellij.psi.PsiElement

sealed class Entity<out T>(val reference: T) {
    class Element(element: PsiElement): Entity<PsiElement>(element)
    class File(file: RefFile): Entity<RefFile>(file)
    class Module(module: RefModule): Entity<RefModule>(module)
    class Directory(directory: RefDirectory): Entity<RefDirectory>(directory)
    class Undefined<T: RefEntity>(entity: T): Entity<RefEntity>(entity)
}