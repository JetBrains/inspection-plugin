package org.jetbrains.intellij

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceSet
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.tasks.*
import java.io.File
import org.gradle.internal.hash.HashUtil.createCompactMD5
import org.jetbrains.intellij.plugins.KotlinPlugin

open class InspectionPlugin : AbstractCodeQualityPlugin<AbstractInspectionsTask, InspectionPluginExtension>() {

    override val toolName: String = "IDEA Inspections"

    override val configurationName: String = SHORT_NAME

    override val reportName: String = SHORT_NAME

    override val sourceBasedTasks = mapOf(
            SHORT_NAME to InspectionsTask::class.java,
            REFORMAT_SHORT_TASK_NAME to ReformatTask::class.java
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
            val version = InspectionPlugin.ideaVersion(extension.idea.version)
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

        internal const val SHORT_NAME = "inspections"

        private const val UNZIP_IDEA_TASK_NAME = "unzip-idea"

        private const val DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME = "download-kotlin-plugin"

        private const val UNZIP_KOTLIN_PLUGIN_TASK_NAME = "unzip-kotlin-plugin"

        private const val IDEA_TASK_NAME = "idea"

        private const val DEFAULT_IDEA_VERSION = "ideaIC:2017.3"

        private const val REFORMAT_SHORT_TASK_NAME = "reformat"

        private val TEMP_DIRECTORY = File(System.getProperty("java.io.tmpdir"))

        internal val BASE_CACHE_DIRECTORY = File(TEMP_DIRECTORY, "inspection-plugin")

        internal val MARKERS_DIRECTORY = File(BASE_CACHE_DIRECTORY, "markers")

        private val DEPENDENCY_SOURCE_DIRECTORY = File(BASE_CACHE_DIRECTORY, "dependencies")

        private val DOWNLOAD_DIRECTORY = File(BASE_CACHE_DIRECTORY, "downloads")

        val IDEA_SYSTEM_DIRECTORY = File(BASE_CACHE_DIRECTORY, "system")

        private val String.normalizedVersion: String
            get() = replace(':', '_').replace('.', '_')

        internal fun ideaVersion(ideaVersion: String?) = ideaVersion ?: DEFAULT_IDEA_VERSION

        internal fun kotlinPluginLocation(version: String?, ideaVersion: String) =
                version?.let { KotlinPlugin.getUrl(it, ideaVersion) }

        internal fun kotlinPluginArchiveDirectory(location: String): File {
            val hash = createCompactMD5(location)
            val name = "kotlin-plugin-$hash"
            return File(DOWNLOAD_DIRECTORY, name)
        }

        internal fun kotlinPluginDirectory(kotlinPluginVersion: String?, ideaVersion: String): File {
            val normIdeaVersion = ideaVersion.normalizedVersion
            val normKotlinPluginVersion = kotlinPluginVersion.toString().normalizedVersion
            val name = "Kotlin-$normKotlinPluginVersion-$normIdeaVersion"
            return File(DEPENDENCY_SOURCE_DIRECTORY, "$name/Kotlin")
        }

        internal fun ideaDirectory(version: String) = File(DEPENDENCY_SOURCE_DIRECTORY, version.normalizedVersion)
    }
}
