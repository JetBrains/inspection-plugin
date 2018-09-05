package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.gradle.internal.hash.HashUtil
import org.jetbrains.intellij.configurations.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.utils.*
import org.jetbrains.intellij.utils.Copy
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipKotlinPluginTask : ConventionTask() {

    @get:Input
    val ideaVersion: String
        get() = ideaVersion(extension.idea.version)

    @get:Input
    @get:Optional
    val version: String?
        get() = extension.plugins.kotlin.version

    @get:Input
    @get:Optional
    val location: String?
        get() = extension.plugins.kotlin.kotlinPluginLocation(ideaVersion)

    @get:InputFile
    @get:Optional
    val archive: File?
        get() = kotlinPluginArchive(location)

    @get:OutputDirectory
    val plugin: File
        get() = extension.plugins.kotlin.kotlinPluginDirectory(ideaVersion)

    @Suppress("unused")
    @TaskAction
    fun apply() {
        if (version == null && location == null) {
            logger.info("InspectionPlugin: Using kotlin plugin inherit from idea.")
            return
        }
        UpToDateChecker(HashUtil.sha256(archive).asHexString()).apply {
            onUpToDate {
                logger.info("InspectionPlugin: No unzipping needed.")
            }
            onNonActual {
                val unzip = Unzip(project)
                val copy = Copy(project)
                val unpacker = Unpacker(logger, unzip, copy)
                unpacker.unpack(archive!!, plugin.parentFile)
            }
        }
    }

    @get:Internal
    private val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!
}