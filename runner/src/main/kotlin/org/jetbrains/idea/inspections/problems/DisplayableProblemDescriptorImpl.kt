package org.jetbrains.idea.inspections.problems

import com.intellij.codeInspection.CommonProblemDescriptor
import com.intellij.codeInspection.reference.RefDirectory
import com.intellij.codeInspection.reference.RefEntity
import com.intellij.codeInspection.reference.RefFile
import com.intellij.codeInspection.reference.RefModule

class DisplayableProblemDescriptorImpl<T : RefEntity>(
        descriptor: CommonProblemDescriptor,
        refEntity: T,
        override val displayName: String,
        override val level: ProblemLevel
) : CommonProblemDescriptor by descriptor, DisplayableProblemDescriptor<T> {

    @Suppress("UNCHECKED_CAST")
    override val entity = when (refEntity) {
        is RefFile -> Entity.File(refEntity) as Entity<T>
        is RefModule -> Entity.Module(refEntity) as Entity<T>
        is RefDirectory -> Entity.Directory(refEntity) as Entity<T>
        else -> Entity.Undefined(refEntity) as Entity<T>
    }

    override fun render(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        fun <T : RefEntity> createIfProblem(
                descriptor: CommonProblemDescriptor,
                refEntity: T,
                displayName: String,
                level: ProblemLevel
        ) = DisplayableProblemDescriptorImpl(descriptor, refEntity, displayName, level)
    }
}