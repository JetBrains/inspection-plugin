package org.jetbrains.idea.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

class InspectionRunner(
        inspectionClass: Class<LocalInspectionTool>
) {
    private val inspection: LocalInspectionTool = inspectionClass.newInstance()

    @Suppress("UNCHECKED_CAST")
    constructor(inspectionClassName: String): this(Class.forName(inspectionClassName) as Class<LocalInspectionTool>)

    fun analyze(file: PsiFile): List<ProblemDescriptor> {
        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val visitor = inspection.buildVisitor(holder, false)
        visitor.visitFile(file)
        return holder.results
    }
}