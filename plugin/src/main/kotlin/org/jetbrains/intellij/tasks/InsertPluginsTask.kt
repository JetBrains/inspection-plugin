package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import java.io.File

open class InsertPluginsTask : ConventionTask() {

    @get:Input
    val ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(extension.ideaVersion)

    @get:Input
    @get:Optional
    val kotlinPluginVersion: KotlinPluginVersion?
        get() = InspectionPlugin.kotlinPluginVersion(extension.kotlinPluginVersion, extension.kotlinPluginLocation)

    @get:InputDirectory
    val ideaDirectory: File
        get() = InspectionPlugin.ideaDirectory(ideaVersion)

    @get:InputDirectory
    @get:Optional
    val kotlinPluginDirectory: File?
        get() = kotlinPluginVersion?.let { InspectionPlugin.kotlinPluginDirectory(it) }

    @get:OutputDirectory
    val destinationIdeaDirectory: File
        get() = InspectionPlugin.ideaDirectory(ideaVersion, kotlinPluginVersion)

    @Suppress("unused")
    @TaskAction
    fun insert() {
        val idea = ideaDirectory
        val kotlinPlugin = kotlinPluginDirectory
        if (isStored(idea, kotlinPlugin)) {
            logger.info("InspectionPlugin: Kotlin plugin inserting not needed.")
            return
        }
        val destination = destinationIdeaDirectory
        if (destination.exists())
            destination.deleteRecursively()
        project.copy {
            it.from(idea)
            it.into(destination)
        }
        val pluginsDirectory = InspectionPlugin.pluginsDirectory(destination)
        val ideaKotlinPluginDirectory = File(pluginsDirectory, "Kotlin")
        if (kotlinPlugin == null) {
            logger.info("InspectionPlugin: Using kotlin plugin inherit from idea. No inserting needed.")
            store(idea, kotlinPlugin)
            if (ideaKotlinPluginDirectory.exists()) return
            logger.error("InspectionPlugin: Kotlin plugin not found. Try specify kotlin plugin version.")
            throw IllegalArgumentException("InspectionPlugin: Kotlin plugin not found. Try specify kotlin plugin version.")
        }
        val kotlinPluginFile = File(kotlinPlugin, "Kotlin/lib/kotlin-plugin.jar")
        val ideaKotlinPluginFile = File(ideaKotlinPluginDirectory, "Kotlin/lib/kotlin-plugin.jar")
        if (ideaKotlinPluginFile.exists()) ideaKotlinPluginFile.delete()
        kotlinPluginFile.compareTo(ideaKotlinPluginFile)
        store(idea, kotlinPlugin)
    }

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!

    private val markerFile: File
        get() = File(InspectionPlugin.BASE_CACHE_DIRECTORY, "insert.cache")

    private val File?.globalIdentifier: String
        get() = this?.name.toString()

    private fun isStored(idea: File, kotlinPlugin: File?): Boolean {
        val marker = markerFile
        val globalIdentifier = "[${idea.globalIdentifier}][${kotlinPlugin.globalIdentifier}]"
        return marker.exists() && marker.readLines().find { it == globalIdentifier } != null
    }

    private fun store(idea: File, kotlinPlugin: File?) = markerFile.let {
        if (!it.exists()) it.createNewFile()
        logger.warn("InspectionPlugin: Marker file updated at $it")
        val globalIdentifier = "[${idea.globalIdentifier}][${kotlinPlugin.globalIdentifier}]"
        it.appendText(globalIdentifier + '\n')
    }
}