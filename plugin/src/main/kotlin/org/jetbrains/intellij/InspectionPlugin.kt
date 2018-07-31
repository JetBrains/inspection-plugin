package org.jetbrains.intellij

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.plugins.quality.internal.AbstractCodeQualityPlugin
import org.gradle.api.tasks.SourceSet
import org.jetbrains.intellij.tasks.*
import java.io.File

open class InspectionPlugin : AbstractCodeQualityPlugin<InspectionsTask>() {

    private val inspectionExtension: InspectionPluginExtension
        get() = extension as InspectionPluginExtension

    override fun getToolName(): String = "IDEA Inspections"

    override fun getTaskType(): Class<InspectionsTask> = InspectionsTask::class.java

    override fun getConfigurationName(): String = SHORT_NAME

    override fun getTaskBaseName(): String = SHORT_NAME

    override fun getReportName(): String = SHORT_NAME

    override fun createExtension(): CodeQualityExtension {
        val extension = project.extensions.create(SHORT_NAME, InspectionPluginExtension::class.java, project)
        LOG.info("Extension $SHORT_NAME created")
        extension.toolVersion = DEFAULT_IDEA_VERSION
        extension.configDir = File("config/$SHORT_NAME")
        extension.config = File(extension.configDir, "$SHORT_NAME.xml").path
        return extension
    }

    override fun beforeApply() {
        project.repositories.maven { it.setUrl("https://www.jetbrains.com/intellij-repository/releases") }
//        project.tasks.create("unzip-idea", UnzipIdeaTask::class.java)
//        project.tasks.create("download-kotlin-plugin", DownloadKotlinPluginTask::class.java)
//        project.tasks.create("unzip-kotlin-plugin", UnzipKotlinPluginTask::class.java)
//        project.tasks.create(CLEAN_TASK_NAME, CleanTask::class.java)
//        project.tasks.create("reformat", ReformatTask::class.java)
        configureTasksDefaults()
        project.rootProject.plugins.apply("idea")
        for (subProject in project.rootProject.subprojects) {
            subProject.plugins.apply("idea")
        }
    }

    override fun configureTaskDefaults(task: InspectionsTask, baseName: String) {
        val baseType = BaseType(baseName)
        configureReformatTaskDefaults(baseType)
        configureIdeaInspectionsTaskDependencies(task)
        configureDefaultDependencies()
        configureReportsConventionMapping(task, baseName)
    }

    private fun configureTasksDefaults() {
        project.tasks.create(CLEAN_TASK_NAME, CleanTask::class.java)
        project.tasks.create(UNZIP_IDEA_TASK_NAME, UnzipIdeaTask::class.java)
        val downloadKotlinPluginTask = project.tasks.create(DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME, DownloadKotlinPluginTask::class.java)
        val unzipKotlinPluginTask = project.tasks.create(UNZIP_KOTLIN_PLUGIN_TASK_NAME, UnzipKotlinPluginTask::class.java)
        unzipKotlinPluginTask.dependsOn += downloadKotlinPluginTask
    }

    private fun configureReformatTaskDefaults(baseType: BaseType) {
        val unzipIdeaTask = project.tasks.getByName(UNZIP_IDEA_TASK_NAME)
        val unzipKotlinPluginTask = project.tasks.getByName(UNZIP_KOTLIN_PLUGIN_TASK_NAME)
        val reformatTask = project.tasks.create(reformatTaskName(baseType), ReformatTask::class.java)
        reformatTask.dependsOn += unzipIdeaTask
        reformatTask.dependsOn += unzipKotlinPluginTask
    }

    private fun configureIdeaInspectionsTaskDependencies(task: InspectionsTask) {
        task.dependsOn += project.tasks.getByName(UNZIP_IDEA_TASK_NAME)
        task.dependsOn += project.rootProject.tasks.getByName("idea")
        for (subProject in project.rootProject.subprojects) {
            task.dependsOn += subProject.tasks.getByName("idea")
        }
    }

    private fun configureDefaultDependencies() {
        project.configurations.getByName(SHORT_NAME).defaultDependencies {
            it.add(project.dependencies.create("com.jetbrains.intellij.idea:${inspectionExtension.toolVersion}"))
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
        task.source = sourceSet.allSource
        val baseType = task.baseType
        val reformatTask = project.tasks.getByName(reformatTaskName(baseType)) as ReformatTask
        reformatTask.source = sourceSet.allSource
    }

    private val InspectionsTask.baseType: BaseType
        get(): BaseType {
            if (!name.startsWith(SHORT_NAME))
                throw IllegalArgumentException("Inspections task name must be started from '$SHORT_NAME'")
            val baseName = name.drop(SHORT_NAME.length)
            return BaseType(baseName)
        }

    companion object {

        internal const val DEFAULT_IDEA_VERSION = "ideaIC:2017.3"

        internal const val DEFAULT_KOTLIN_PLUGIN_VERSION = "1.2.51-release-Studio3.2-1"

        private val LOG: Logger = Logging.getLogger(InspectionPlugin::class.java)

        internal const val SHORT_NAME = "inspections"

        private const val CLEAN_TASK_NAME = SHORT_NAME + "Clean"

        private const val UNZIP_IDEA_TASK_NAME = "unzip-idea"

        private const val DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME = "download-kotlin-plugin"

        private const val UNZIP_KOTLIN_PLUGIN_TASK_NAME = "unzip-kotlin-plugin"

        private fun reformatTaskName(baseType: BaseType) = "reformat" + baseType.value

        private val TEMP_DIRECTORY: File
            get() = File(System.getProperty("java.io.tmpdir"))

        internal val BASE_CACHE_DIRECTORY: File
            get() = File(TEMP_DIRECTORY, "inspection-plugin")

        internal val DEPENDENCY_SOURCE_DIRECTORY: File
            get() = File(BASE_CACHE_DIRECTORY, "dependencies")

        private val DOWNLOAD_DIRECTORY: File
            get() = File(InspectionPlugin.BASE_CACHE_DIRECTORY, "downloads")

        internal fun kotlinPluginLocation(kotlinPluginVersion: String) = when (kotlinPluginVersion) {
            "1.2.51-release-Studio3.2-1" -> "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=47481"
            else -> throw IllegalArgumentException("Unsupported kotlin plugin version $kotlinPluginVersion")
        }

        internal fun kotlinPluginSource(kotlinPluginVersion: String) =
                File(DOWNLOAD_DIRECTORY, kotlinPluginVersion.normalizeVersion + ".zip")

        internal fun kotlinPluginDirectory(kotlinPluginVersion: String) =
                File(DEPENDENCY_SOURCE_DIRECTORY, kotlinPluginVersion.normalizeVersion)

        internal fun ideaDirectory(ideaVersion: String) =
                File(DEPENDENCY_SOURCE_DIRECTORY, ideaVersion.normalizeVersion)

        private val String.normalizeVersion: String
            get() = replace(':', '_').replace('.', '_').replace('-', '_')
    }
}
