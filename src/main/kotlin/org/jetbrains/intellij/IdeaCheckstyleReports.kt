package org.jetbrains.intellij

import org.gradle.api.Task
import org.gradle.api.plugins.quality.CheckstyleReports
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.reporting.internal.CustomizableHtmlReportImpl
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport
import org.gradle.api.reporting.internal.TaskReportContainer

class IdeaCheckstyleReports(
        task: Task
) : TaskReportContainer<SingleFileReport>(SingleFileReport::class.java, task), CheckstyleReports {

    init {
        add(CustomizableHtmlReportImpl::class.java, "html", task)
        add(TaskGeneratedSingleFileReport::class.java, "xml", task)
    }

    override fun getHtml(): SingleFileReport = getByName("html")

    override fun getXml(): SingleFileReport = getByName("xml")
}
