package org.jetbrains.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.Application
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.gradle.api.reporting.SingleFileReport
import org.jetbrains.intellij.ProblemLevel

class HTMLGenerator(override val report: SingleFileReport, private val application: Application) : ReportGenerator {
    private val sb = StringBuilder()

    private val documentManager = FileDocumentManager.getInstance()

    init {
        sb.appendln("""
<html><head><style>
error {
    background-color: red;
}
warning {
    background-color: yellow;
}
info {
    text-decoration-style: wavy;
    text-decoration: underline;
}
unused {
    background-color: lightgray;
}
keyword {
    font-weight: bold;
}
</style></head>
<body>
""")
    }

    private fun PsiElement.coversElement(problemElement: PsiElement, document: Document?): Boolean {
        if (document == null) {
            return true
        }
        val problemLine = problemElement.getLine(document)
        val line = getLine(document)
        return line < problemLine
    }

    private fun PsiElement.findElementToPrint(document: Document?): PsiElement {
        var elementToPrint = this
        while (elementToPrint.parent != null &&
               elementToPrint.parent !is PsiFile &&
               !elementToPrint.coversElement(this, document)) {

            elementToPrint = elementToPrint.parent
        }
        return elementToPrint
    }

    private fun PsiElement.printSmartly(problemChild: PsiElement, problemTag: String, document: Document?) {
        val printer = StringBuilder()
        val problemLine = problemChild.getLine(document)
        var ellipsisBefore = false
        var ellipsisAfter = false
        this.accept(object : PsiRecursiveElementVisitor() {
            var insideProblemChild = false

            override fun visitElement(element: PsiElement) {
                if (element === problemChild) {
                    printer.append("<$problemTag>")
                    insideProblemChild = true
                }
                super.visitElement(element)
                if (element.firstChild == null) {
                    val elementLine = element.getLine(document)
                    if (insideProblemChild || elementLine in problemLine - 2..problemLine + 2) {
                        val keyword = when (element) {
                            is PsiKeyword -> true
                            is LeafPsiElement -> element.text.isKotlinKeyword()
                            else -> false
                        }
                        if (keyword) {
                            printer.append("<keyword>")
                        }
                        printer.append(element.text)
                        if (keyword) {
                            printer.append("</keyword>")
                        }
                    } else if (elementLine < problemLine - 2) {
                        ellipsisBefore = true
                    } else {
                        ellipsisAfter = true
                    }
                }
                if (element === problemChild) {
                    insideProblemChild = false
                    printer.append("</$problemTag>")
                }
            }
        })
        printer.appendln()
        sb.appendln("<pre>")
        if (ellipsisBefore) {
            sb.appendln("...")
        }
        sb.append(printer)
        if (ellipsisAfter) {
            sb.appendln("...")
        }
        sb.appendln("</pre>")
    }

    override fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String) {
        sb.appendln("<p>")
        sb.appendln("    In file <b>${problem.renderLocation()}</b>:")
        sb.appendln("</p>")

        val psiElement = problem.psiElement
        application.runReadAction {
            val problemTag = when (problem.highlightType) {
                ProblemHighlightType.LIKE_UNUSED_SYMBOL -> "unused"
                else -> when (level) {
                    ProblemLevel.ERROR -> "error"
                    ProblemLevel.WARNING -> "warning"
                    ProblemLevel.WEAK_WARNING, ProblemLevel.INFORMATION -> "info"
                }
            }
            val document = psiElement?.containingFile?.virtualFile?.let { documentManager.getDocument(it) }
            psiElement?.findElementToPrint(document)?.printSmartly(psiElement, problemTag, document)
        }
        sb.appendln("<p>")
        sb.appendln("    <i>${problem.render()}</i>")
        sb.appendln("</p>")
    }

    private fun closeHtml() {
        sb.appendln("</body></html>")
    }

    override fun generate() {
        closeHtml()

        val htmlReportFile = report.destination
        htmlReportFile.writeText(sb.toString())
    }
}