package org.jetbrains.intellij

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceSet
import org.jetbrains.intellij.configurations.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.tasks.*
import java.io.File
import java.net.URLClassLoader

open class InspectionPlugin : AbstractCodeQualityPlugin<AbstractInspectionsTask, InspectionPluginExtension>() {

    override val toolName: String = "IDEA Inspections"

    override val configurationName: String = SHORT_NAME

    override val reportName: String = SHORT_NAME

    override val sourceBasedTasks = listOf(
            TaskDescriptor(CHECK_TASK_NAME, CheckInspectionsTask::class.java, false),
            TaskDescriptor(SHORT_NAME, InspectionsTask::class.java, true),
            TaskDescriptor(REFORMAT_SHORT_TASK_NAME, ReformatTask::class.java, true)
    )

    override fun createExtension(): InspectionPluginExtension {
        val extension = project.extensions.create(SHORT_NAME, InspectionPluginExtension::class.java, project)
        logger.info("InspectionPlugin: Extension $SHORT_NAME created")
        return extension
    }

    override fun beforeApply() {
        project.repositories.maven { it.setUrl("https://www.jetbrains.com/intellij-repository/releases") }
        configureTasksDefaults()
        project.rootProject.plugins.apply("idea")
        for (subProject in project.rootProject.subprojects) {
            subProject.plugins.apply("idea")
        }
    }

    override fun configureConfiguration(configuration: Configuration) {
        configuration.defaultDependencies {
            val version = ideaVersion(extension.idea.version)
            it.add(project.dependencies.create("com.jetbrains.intellij.idea:$version"))
        }
    }

    override fun configureTaskDefaults(task: AbstractInspectionsTask, baseName: String) {
        configureTaskDependencies(task)
        if (task is InspectionsTask) configureReportsConventionMapping(task, baseName)
    }

    private fun configureTasksDefaults() {
        with(project) {
            tasks.create(UNZIP_IDEA_TASK_NAME, UnzipIdeaTask::class.java)
            tasks.create(DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME, DownloadKotlinPluginTask::class.java)
            tasks.create(UNZIP_KOTLIN_PLUGIN_TASK_NAME, UnzipKotlinPluginTask::class.java) {
                it.dependsOn += tasks.getByName(DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME)
            }
        }
    }

    private fun configureTaskDependencies(task: AbstractInspectionsTask) {
        task.dependsOn += project.tasks.getByName(UNZIP_IDEA_TASK_NAME)
        task.dependsOn += project.tasks.getByName(UNZIP_KOTLIN_PLUGIN_TASK_NAME)
        task.dependsOn += project.rootProject.tasks.getByName(IDEA_TASK_NAME)
        for (subProject in project.rootProject.subprojects) {
            task.dependsOn += subProject.tasks.getByName(IDEA_TASK_NAME)
        }
    }

    private fun configureReportsConventionMapping(task: InspectionsTask, baseName: String) {
        task.reports.all { report ->
            val reportMapping = AbstractCodeQualityPlugin.conventionMappingOf(report)
            reportMapping.map("enabled") { true }
            reportMapping.map("destination") {
                File(extension.reportsDir, "$baseName.${report.name}")
            }
        }
    }

    override fun configureForSourceSet(sourceSet: SourceSet, task: AbstractInspectionsTask) {
        task.description = "Run IDEA inspections for " + sourceSet.name + " classes"
        task.classpath = sourceSet.output.plus(sourceSet.compileClasspath)
        task.setSourceSet(sourceSet.allSource)
    }

    companion object {
        private val logger: Logger = Logging.getLogger(InspectionPlugin::class.java)
    }
}
