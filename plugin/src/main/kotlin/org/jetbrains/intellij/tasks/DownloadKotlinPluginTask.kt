package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.ExceptionHandler
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.configurations.KotlinPlugin
import org.jetbrains.intellij.utils.Downloader
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class DownloadKotlinPluginTask : ConventionTask() {

    @get:Input
    val ideaVersion: String
        get() = InspectionPlugin.ideaVersion(extension.idea.version)

    @get:Input
    @get:Optional
    val version: String?
        get() = extension.plugins.kotlin.version

    @get:Input
    @get:Optional
    val location: String?
        get() = extension.plugins.kotlin.location ?: InspectionPlugin.kotlinPluginLocation(version, ideaVersion)

    @get:OutputDirectory
    @get:Optional
    val archiveDirectory: File?
        get() = location?.let { InspectionPlugin.kotlinPluginArchiveDirectory(it) }

    @Suppress("unused")
    @TaskAction
    fun apply() {
        val version = version
        val location = location
        if (version == null && location == null) {
            logger.info("InspectionPlugin: Using kotlin plugin inherit from idea. No downloading needed.")
            return
        }
        if (version == null) ExceptionHandler.exception(this, "Expected version for kotlin plugin $location")
        if (location == null) ExceptionHandler.exception(this, "Expected url for kotlin plugin $version")
        KotlinPlugin.checkCompatibility(this, version, ideaVersion)
        Downloader(logger).download(location, archiveDirectory!!)
    }

    @get:Internal
    private val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!
}
