package org.jetbrains.idea.inspections

import org.gradle.api.reporting.SingleFileReport
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.XMLOutputter
import org.jetbrains.intellij.ProblemLevel

class XMLGenerator(private val report: SingleFileReport) {

    private val errorsRoot = Element("errors")
    private val errorElements = mutableListOf<Element>()
    private val warningsRoot = Element("warnings")
    private val warningElements = mutableListOf<Element>()
    private val infosRoot = Element("infos")
    private val infoElements = mutableListOf<Element>()

    fun report(problem: PinnedProblemDescriptor, level: ProblemLevel, inspectionClass: String) {
        val element = Element("problem")
        element.addContent(Element("file").addContent(problem.fileName))
        element.addContent(Element("line").addContent((problem.line + 1).toString()))
        element.addContent(Element("row").addContent((problem.row + 1).toString()))
        element.addContent(Element("java_class").addContent(inspectionClass))
        element.addContent(Element("problem_class")
                .setAttribute("severity", level.name)
                .addContent(problem.displayName ?: "<ANONYMOUS>"))
        element.addContent(Element("description").addContent(problem.render()))
        when (level) {
            ProblemLevel.ERROR -> errorElements += element
            ProblemLevel.WARNING, ProblemLevel.WEAK_WARNING -> warningElements += element
            ProblemLevel.INFORMATION -> infoElements += element
        }
    }

    fun generate() {
        val xmlReportFile = report.destination
        errorsRoot.setContent(errorElements)
        warningsRoot.setContent(warningElements)
        infosRoot.setContent(infoElements)
        val root = Element("report")
        root.addContent(errorsRoot)
        root.addContent(warningsRoot)
        root.addContent(infosRoot)
        val document = Document(root)
        XMLOutputter().output(document, xmlReportFile.outputStream())
    }
}