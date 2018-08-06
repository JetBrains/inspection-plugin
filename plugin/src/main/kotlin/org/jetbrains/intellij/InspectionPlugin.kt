package org.jetbrains.intellij

import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.plugins.quality.internal.AbstractCodeQualityPlugin
import org.gradle.api.tasks.SourceSet
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.tasks.*
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import java.io.File

open class InspectionPlugin : AbstractCodeQualityPlugin<InspectionsTask>() {

    override fun configureConfiguration(configuration: Configuration) {}

    private val inspectionExtension: InspectionsExtension
        get() = extension as InspectionsExtension

    override fun getToolName(): String = "IDEA Inspections"

    override fun getTaskType(): Class<InspectionsTask> = InspectionsTask::class.java

    override fun getConfigurationName(): String = SHORT_NAME

    override fun getTaskBaseName(): String = SHORT_NAME

    override fun getReportName(): String = SHORT_NAME

    override fun createExtension(): CodeQualityExtension {
        val extension = project.extensions.create(SHORT_NAME, InspectionsExtension::class.java, project)
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

    override fun configureTaskDefaults(task: InspectionsTask, baseName: String) {
        val baseType = BaseType(baseName)
        task.baseType = baseType
        configureReformatTaskDefaults(baseType)
        configureIdeaInspectionsTaskDependencies(task)
        configureDefaultDependencies(task)
        configureReportsConventionMapping(task, baseName)
    }

    private fun configureTasksDefaults() {
        with(project) {
            tasks.create(UNZIP_IDEA_TASK_NAME, UnzipIdeaTask::class.java)
            tasks.create(DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME, DownloadKotlinPluginTask::class.java)
            tasks.create(UNZIP_KOTLIN_PLUGIN_TASK_NAME, UnzipKotlinPluginTask::class.java) {
                it.dependsOn += tasks.getByName(DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME)
            }
            tasks.create(INSERT_PLUGINS_TASK_NAME, InsertPluginsTask::class.java) {
                it.dependsOn += tasks.getByName(UNZIP_KOTLIN_PLUGIN_TASK_NAME)
                it.dependsOn += tasks.getByName(UNZIP_IDEA_TASK_NAME)
            }
        }
    }

    private fun configureReformatTaskDefaults(baseType: BaseType) {
        project.tasks.create(reformatTaskName(baseType), ReformatTask::class.java) {
            it.baseType = baseType
            configureIdeaInspectionsTaskDependencies(it)
        }
    }

    private fun configureIdeaInspectionsTaskDependencies(task: AbstractInspectionsTask) {
        task.dependsOn += project.tasks.getByName(INSERT_PLUGINS_TASK_NAME)
        task.dependsOn += project.rootProject.tasks.getByName(IDEA_TASK_NAME)
        for (subProject in project.rootProject.subprojects) {
            task.dependsOn += subProject.tasks.getByName(IDEA_TASK_NAME)
        }
    }

    private fun configureDefaultDependencies(task: InspectionsTask) {
        project.configurations.getByName(SHORT_NAME).defaultDependencies {
            it.add(project.dependencies.create(task.ideaVersion.mavenUrl))
        }
    }

    private fun configureReportsConventionMapping(task: InspectionsTask, baseName: String) {
        task.reports.all { report ->
            val reportMapping = AbstractCodeQualityPlugin.conventionMappingOf(report)
            reportMapping.map("enabled") { true }
            reportMapping.map("destination") {
                File(inspectionExtension.reportsDir, "$baseName.${report.name}")
            }
        }
    }

    override fun configureForSourceSet(sourceSet: SourceSet, task: InspectionsTask) {
        task.description = "Run IDEA inspections for " + sourceSet.name + " classes"
        task.classpath = sourceSet.output.plus(sourceSet.compileClasspath)
        task.setSourceSet(sourceSet.allSource)
        val baseType = task.baseType
        val reformatTask = project.tasks.getByName(reformatTaskName(baseType)) as ReformatTask
        reformatTask.setSourceSet(sourceSet.allSource)
    }

    companion object {

        private val logger: Logger = Logging.getLogger(InspectionPlugin::class.java)

        internal const val SHORT_NAME = "inspections"

        private const val UNZIP_IDEA_TASK_NAME = "unzip-idea"

        private const val DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME = "download-kotlin-plugin"

        private const val UNZIP_KOTLIN_PLUGIN_TASK_NAME = "unzip-kotlin-plugin"

        private const val INSERT_PLUGINS_TASK_NAME = "insert-plugins"

        private const val IDEA_TASK_NAME = "idea"

        private fun reformatTaskName(baseType: BaseType) = "reformat" + baseType.baseTitle

        private val TEMP_DIRECTORY = File(System.getProperty("java.io.tmpdir"))

        internal val BASE_CACHE_DIRECTORY = File(TEMP_DIRECTORY, "inspection-plugin")

        private val DEPENDENCY_SOURCE_DIRECTORY = File(BASE_CACHE_DIRECTORY, "dependencies")

        private val DOWNLOAD_DIRECTORY = File(BASE_CACHE_DIRECTORY, "downloads")

        internal fun ideaDirectory(ideaVersion: IdeaVersion, kotlinPluginVersion: KotlinPluginVersion?) =
                File(DEPENDENCY_SOURCE_DIRECTORY,
                        "[${ideaVersion.normalizeVersion}][${kotlinPluginVersion.normalizeVersion}]")

        internal fun kotlinPluginSource(kotlinPluginVersion: KotlinPluginVersion) =
                File(DOWNLOAD_DIRECTORY, kotlinPluginVersion.normalizeVersion + ".zip")

        internal fun kotlinPluginDirectory(kotlinPluginVersion: KotlinPluginVersion) =
                File(DEPENDENCY_SOURCE_DIRECTORY, kotlinPluginVersion.normalizeVersion)

        internal fun pluginsDirectory(ideaDirectory: File) =
                File(ideaDirectory, "plugins")

        internal fun ideaDirectory(ideaVersion: IdeaVersion) =
                File(DEPENDENCY_SOURCE_DIRECTORY, ideaVersion.normalizeVersion)

        private val IdeaVersion.normalizeVersion: String
            get() = value.normalizeVersion

        private val KotlinPluginVersion?.normalizeVersion: String
            get() = this?.value?.normalizeVersion ?: ""

        private val String.normalizeVersion: String
            get() = replace(':', '_').replace('.', '_').replace('-', '_')

        internal fun ideaVersion(ideaVersion: String?): IdeaVersion {
            if (ideaVersion == null) return IdeaVersion.IDEA_IC_2017_3
            val version = IdeaVersion(ideaVersion)
            if (version is IdeaVersion.Other)
                logger.warn("InspectionPlugin: Uses custom idea version: $version")
            return version
        }

        internal fun kotlinPluginVersion(kotlinPluginVersion: String?, url: String?): KotlinPluginVersion? {
            if (kotlinPluginVersion == null) return null
            val version = KotlinPluginVersion(kotlinPluginVersion, url)
            if (version is KotlinPluginVersion.Other) {
                logger.warn("InspectionPlugin: Uses custom kotlin plugin version $version")
            } else if (url != null) {
                logger.warn("InspectionPlugin: Uses custom kotlin plugin sources for defined version $version")
            }
            return version
        }
    }
}
