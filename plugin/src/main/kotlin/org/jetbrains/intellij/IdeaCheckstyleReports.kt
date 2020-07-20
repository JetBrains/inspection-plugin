package org.jetbrains.intellij

import org.gradle.api.Task
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.plugins.quality.CheckstyleReports
import org.gradle.api.reporting.CustomizableHtmlReport
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.reporting.internal.CustomizableHtmlReportImpl
import org.gradle.api.reporting.internal.TaskGeneratedSingleFileReport
import org.gradle.api.reporting.internal.TaskReportContainer

class IdeaCheckstyleReports(
        task: Task
) : TaskReportContainer<SingleFileReport>(SingleFileReport::class.java, task, CollectionCallbackActionDecorator.NOOP), CheckstyleReports {

    init {
        add(CustomizableHtmlReportImpl::class.java, "html", task)
        add(TaskGeneratedSingleFileReport::class.java, "xml", task)
    }

    override fun getHtml(): CustomizableHtmlReport = getByName("html") as CustomizableHtmlReport

    override fun getXml(): SingleFileReport = getByName("xml")
}
