package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.configurations.*
import org.jetbrains.intellij.exception
import org.jetbrains.intellij.utils.Downloader
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class DownloadKotlinPluginTask : ConventionTask() {

    @get:Input
    val ideaVersion: String
        get() = ideaVersion(extension.idea.version)

    @get:Input
    val isTempDirInHome: Boolean
        get() = extension.isTempDirInHome()

    @get:Input
    @get:Optional
    val version: String?
        get() = extension.plugins.kotlin.version

    @get:Input
    @get:Optional
    val location: String?
        get() = extension.plugins.kotlin.kotlinPluginLocation(ideaVersion)

    @get:OutputDirectory
    @get:Optional
    val archiveDirectory: File?
        get() = kotlinPluginArchiveDirectory(location, isTempDirInHome)

    @Suppress("unused")
    @TaskAction
    fun apply() {
        val version = version
        val location = location
        if (version == null && location == null) {
            logger.info("InspectionPlugin: Using kotlin plugin inherit from idea. No downloading needed.")
            return
        }
        if (location == null) {
            exception(this, "Expected url for kotlin plugin $version")
        }
        Downloader(logger).download(location, isTempDirInHome, archiveDirectory!!)
    }

    @get:Internal
    private val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!
}
