package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.configurations.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.utils.Copy
import org.jetbrains.intellij.utils.Unpacker
import org.jetbrains.intellij.utils.Unzip
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipKotlinPluginTask : ConventionTask() {

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
        get() = extension.plugins.kotlin.location ?: kotlinPluginLocation(version, ideaVersion)

    @get:InputFile
    @get:Optional
    val archive: File?
        get() = location?.let { kotlinPluginDownloadDirectory(it, isTempDirInHome).listFiles()?.firstOrNull() }

    @get:OutputDirectory
    val plugin: File
        get() = kotlinPluginDirectory(version, ideaVersion, isTempDirInHome)

    @Suppress("unused")
    @TaskAction
    fun apply() {
        val version = version
        val location = location
        if (version == null && location == null) {
            logger.info("InspectionPlugin: Using kotlin plugin inherit from idea. No unzipping needed.")
            return
        }
        val unzip = Unzip(project)
        val copy = Copy(project)
        val unpacker = Unpacker(logger, unzip, copy)
        unpacker.unpack(archive!!, isTempDirInHome, plugin.parentFile)
    }

    @get:Internal
    private val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!
}