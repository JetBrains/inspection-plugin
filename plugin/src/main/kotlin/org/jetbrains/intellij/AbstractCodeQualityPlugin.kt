package org.jetbrains.intellij

import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import java.util.*
import java.util.concurrent.Callable

abstract class AbstractCodeQualityPlugin<T : Task, E : CodeQualityExtension> : Plugin<ProjectInternal> {

    protected lateinit var project: ProjectInternal

    protected lateinit var extension: E

    protected abstract val toolName: String

    protected abstract val configurationName: String

    protected abstract val reportName: String

    protected open val basePlugin: Class<out Plugin<*>> = JavaBasePlugin::class.java

    private val javaPluginConvention: JavaPluginConvention
        get() = project.convention.getPlugin(JavaPluginConvention::class.java)

    protected abstract val sourceBasedTasks: Map<String, Class<out T>>

    protected abstract fun beforeApply()

    protected abstract fun createExtension(): E

    protected abstract fun configureConfiguration(configuration: Configuration)

    protected abstract fun configureTaskDefaults(task: T, baseName: String)

    protected abstract fun configureForSourceSet(sourceSet: SourceSet, task: T)

    override fun apply(project: ProjectInternal) {
        this.project = project

        beforeApply()
        project.pluginManager.apply(ReportingBasePlugin::class.java)
        createConfigurations()
        extension = createExtension()
        configureExtensionRule()
        configureTaskRule()
        configureSourceSetRule()
        configureCheckTask()
    }

    private fun createConfigurations() {
        val configuration = project.configurations.create(configurationName)
        configuration.isVisible = false
        configuration.isTransitive = true
        configuration.description = "The $toolName libraries to be used for this project."
        // Don't need these things, they're provided by the runtime
        configuration.exclude(excludeProperties("ant", "ant"))
        configuration.exclude(excludeProperties("org.apache.ant", "ant"))
        configuration.exclude(excludeProperties("org.apache.ant", "ant-launcher"))
        configuration.exclude(excludeProperties("org.slf4j", "slf4j-api"))
        configuration.exclude(excludeProperties("org.slf4j", "jcl-over-slf4j"))
        configuration.exclude(excludeProperties("org.slf4j", "log4j-over-slf4j"))
        configuration.exclude(excludeProperties("commons-logging", "commons-logging"))
        configuration.exclude(excludeProperties("log4j", "log4j"))
        configureConfiguration(configuration)
    }

    private fun excludeProperties(group: String, module: String): Map<String, String> {
        return mapOf("group" to group, "module" to module)
    }

    private fun configureExtensionRule() {
        val conventionMapping = conventionMappingOf(extension)
        conventionMapping.map("sourceSets") { ArrayList<Any>() }
        conventionMapping.map("reportsDir") {
            project.extensions.getByType(ReportingExtension::class.java).file(reportName)
        }
        withBasePlugin { conventionMapping.map("sourceSets") { javaPluginConvention.sourceSets } }
    }

    private fun configureTaskRule() {
        for ((taskBaseName, taskType) in sourceBasedTasks) {
            project.tasks.withType(taskType).all { task: T ->
                var prunedName = task.name.replaceFirst(taskBaseName.toRegex(), "")
                if (prunedName.isEmpty()) {
                    prunedName = task.name
                }
                prunedName = ("" + prunedName[0]).toLowerCase() + prunedName.substring(1)
                configureTaskDefaults(task, prunedName)
            }
        }
    }

    private fun configureSourceSetRule() {
        withBasePlugin { configureForSourceSets(javaPluginConvention.sourceSets) }
    }

    private fun configureForSourceSets(sourceSets: SourceSetContainer) {
        for ((taskBaseName, taskType) in sourceBasedTasks) {
            sourceSets.all { sourceSet ->
                val taskName = sourceSet.getTaskName(taskBaseName, null)
                project.tasks.create(taskName, taskType) {
                    configureForSourceSet(sourceSet, it)
                }
            }
        }
    }

    private fun configureCheckTask() {
        withBasePlugin { configureCheckTaskDependents() }
    }

    private fun configureCheckTaskDependents() {
        val tasksBaseNames = sourceBasedTasks.keys
        project.tasks.getByName(JavaBasePlugin.CHECK_TASK_NAME) { task ->
            task.dependsOn(Callable {
                tasksBaseNames.map { taskBaseName ->
                    extension.sourceSets.map {
                        it.getTaskName(taskBaseName, null)
                    }
                }.flatten()
            })
        }
    }

    private fun withBasePlugin(action: (Plugin<*>) -> Unit) {
        project.plugins.withType(basePlugin, action)
    }

    companion object {
        fun conventionMappingOf(obj: Any): ConventionMapping {
            return (obj as IConventionAware).conventionMapping
        }
    }
}