package org.jetbrains.intellij

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.plugins.quality.internal.AbstractCodeQualityPlugin
import org.gradle.api.tasks.SourceSet
import java.io.File

open class InspectionPlugin : AbstractCodeQualityPlugin<Inspection>() {

    private val inspectionExtension: InspectionPluginExtension get() = extension as InspectionPluginExtension

    override fun getToolName(): String = "IDEA Inspections"

    override fun getTaskType(): Class<Inspection> = Inspection::class.java

    override fun getConfigurationName(): String = SHORT_NAME

    override fun getTaskBaseName(): String = SHORT_NAME

    override fun getReportName(): String = SHORT_NAME

    override fun createExtension(): CodeQualityExtension {
        val extension = project.extensions.create(SHORT_NAME, InspectionPluginExtension::class.java, project)
        extension.toolVersion = DEFAULT_IDEA_VERSION

        extension.configDir = File("config/$SHORT_NAME")
        extension.config = File(extension.configDir, "$SHORT_NAME.xml").path
        return extension
    }

    override fun beforeApply() {
        super.beforeApply()

        project.repositories.maven { it.setUrl("https://www.jetbrains.com/intellij-repository/releases") }
        project.tasks.create("unzip", UnzipTask::class.java)
        project.tasks.create(CLEAN_TASK_NAME, CleanTask::class.java)
        project.rootProject.plugins.apply("idea")
        project.plugins.apply("idea")
    }

    override fun configureTaskDefaults(task: Inspection, baseName: String) {
        val configuration = project.configurations.getAt(SHORT_NAME)

        val unzipTask = project.tasks.getAt("unzip")
        task.dependsOn += unzipTask
        val rootIdeaTask = project.rootProject.tasks.getAt("idea")
        task.dependsOn += rootIdeaTask
        val ideaTask = project.tasks.getAt("idea")
        task.dependsOn += ideaTask

        configureDefaultDependencies(configuration)
        configureReportsConventionMapping(task, baseName)
    }

    private fun configureDefaultDependencies(configuration: Configuration) {
        configuration.defaultDependencies { dependencies ->
            LOG.info("XXXXXXXXXXX Configuring default dependencies for configuration ${configuration.name}")
            dependencies.add(project.dependencies.create("com.jetbrains.intellij.idea:${inspectionExtension.toolVersion}"))
        }
    }

    private fun configureReportsConventionMapping(task: Inspection, baseName: String) {
        task.reports.all { report ->
            val reportMapping = AbstractCodeQualityPlugin.conventionMappingOf(report)
            reportMapping.map("enabled") { true }
            reportMapping.map("destination") {
                File(inspectionExtension.reportsDir, "$baseName.${report.name}")
            }
        }
    }

    override fun configureForSourceSet(sourceSet: SourceSet, task: Inspection) {
        task.description = "Run IDEA inspections for " + sourceSet.name + " classes"
        task.classpath = sourceSet.output.plus(sourceSet.compileClasspath)
        task.setSourceSet(sourceSet.allSource)
    }

    companion object {

        const val DEFAULT_IDEA_VERSION = "ideaIC:2017.3"

        private val LOG: Logger = Logging.getLogger(InspectionPlugin::class.java)

        internal const val SHORT_NAME = "inspections"

        private const val CLEAN_TASK_NAME = SHORT_NAME + "Clean"
    }
}
