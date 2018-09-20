package org.jetbrains.idea.inspections.generators

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.idea.inspections.*
import org.jetbrains.idea.inspections.problems.DisplayableProblemDescriptor
import org.jetbrains.idea.inspections.problems.PinnedProblemDescriptor
import org.jetbrains.idea.inspections.problems.ProblemLevel
import java.io.File
import java.util.*

class HTMLGenerator(override val reportFile: File) : ReportGenerator {
    private val problems = ArrayList<DisplayableProblemDescriptor<*>>()

    private fun HTML.generateHead() {
        tag("style") {
            text("""
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
            """.trimIndent())
        }
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

    private fun HTML.printSmartly(e: PsiElement, problemChild: PsiElement, problemTag: String, document: Document?) {
        val problemLine = problemChild.getLine(document)
        var ellipsisBefore = false
        var ellipsisAfter = false
        tag("pre") {
            val snapshot = snapshot()
            snapshot.raw {
                e.accept(object : PsiRecursiveElementVisitor() {
                    var insideProblemChild = false

                    override fun visitElement(element: PsiElement) {
                        val isProblemElement = element === problemChild
                        tagIf(isProblemElement, problemTag) {
                            if (isProblemElement) insideProblemChild = true
                            super.visitElement(element)
                            if (element.firstChild == null) {
                                val elementLine = element.getLine(document)
                                if (insideProblemChild || elementLine in problemLine - 2..problemLine + 2) {
                                    val keyword = when (element) {
                                        is PsiKeyword -> true
                                        is LeafPsiElement -> element.text.isKotlinKeyword()
                                        else -> false
                                    }
                                    tagIf(keyword, "keyword") {
                                        text(element.text)
                                    }
                                } else if (elementLine < problemLine - 2) {
                                    ellipsisBefore = true
                                } else {
                                    ellipsisAfter = true
                                }
                            }
                            if (isProblemElement) insideProblemChild = false
                        }
                    }
                })
            }
            if (ellipsisBefore) raw {
                text("...")
            }
            merge(snapshot)
            if (ellipsisAfter) raw {
                text("...")
            }
        }
    }

    private fun HTML.generatePinnedProblem(problem: PinnedProblemDescriptor, documentManager: FileDocumentManager) {
        tag("p") {
            line {
                text("In file ")
                tag("b") {
                    text(problem.renderLocation())
                }
                text(":")
            }
        }

        val psiElement = problem.psiElement
        val problemTag = when (problem.highlightType) {
            ProblemHighlightType.LIKE_UNUSED_SYMBOL -> "unused"
            else -> when (problem.level) {
                ProblemLevel.ERROR -> "error"
                ProblemLevel.WARNING -> "warning"
                ProblemLevel.WEAK_WARNING, ProblemLevel.INFO -> "info"
            }
        }
        val document = psiElement?.containingFile?.virtualFile?.let { documentManager.getDocument(it) }
        psiElement?.findElementToPrint(document)?.let {
            printSmartly(it, psiElement, problemTag, document)
        }

        tag("p") {
            line {
                tag("i") {
                    text(problem.renderDescription())
                }
            }
        }
    }

    private fun HTML.generateProblem(problem: DisplayableProblemDescriptor<*>) {
        tag("p") {
            line {
                tag("i") {
                    text(problem.render())
                }
            }
        }
    }

    private fun HTML.generateBody() {
        val documentManager = FileDocumentManager.getInstance()
        for (problem in problems) when (problem) {
            is PinnedProblemDescriptor -> generatePinnedProblem(problem, documentManager)
            else -> generateProblem(problem)
        }
    }

    override fun report(problem: DisplayableProblemDescriptor<*>, inspectionClass: String) {
        problems.add(problem)
    }

    override fun generate() {
        val html = HTML()
        html.tag("html") {
            tag("head") {
                generateHead()
            }
            tag("body") {
                generateBody()
            }
        }
        reportFile.writeText(html.result)
    }
}