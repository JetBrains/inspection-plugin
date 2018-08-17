package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.utils.Copy
import org.jetbrains.intellij.utils.Unpacker
import org.jetbrains.intellij.utils.Unzip
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipKotlinPluginTask : ConventionTask() {

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

    @get:InputFile
    @get:Optional
    val archive: File?
        get() = location?.let { InspectionPlugin.kotlinPluginArchiveDirectory(it).listFiles()?.firstOrNull() }

    @get:OutputDirectory
    val plugin: File
        get() = InspectionPlugin.kotlinPluginDirectory(version, ideaVersion)

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
        unpacker.unpack(archive!!, plugin.parentFile)
    }

    @get:Internal
    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!
}