package org.jetbrains.idea.inspections.problems

import com.intellij.codeInspection.CommonProblemDescriptor


interface DisplayableProblemDescriptor<T> : CommonProblemDescriptor {
    val level: ProblemLevel

    val displayName: String

    val entity: Entity<T>?

    fun render(): String
}