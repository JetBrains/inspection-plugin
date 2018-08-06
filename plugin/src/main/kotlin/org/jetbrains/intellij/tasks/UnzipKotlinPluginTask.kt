package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.utils.Copy
import org.jetbrains.intellij.utils.Unpacker
import org.jetbrains.intellij.utils.Unzip
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipKotlinPluginTask : ConventionTask() {

    @get:Input
    val ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(extension.ideaVersion)

    @get:Input
    @get:Optional
    val kotlinPluginVersion: KotlinPluginVersion?
        get() = InspectionPlugin.kotlinPluginVersion(extension.kotlinPluginVersion, extension.kotlinPluginLocation)

    @get:InputFile
    @get:Optional
    val sourceFile: File?
        get() = kotlinPluginVersion?.let { InspectionPlugin.kotlinPluginSource(it) }

    @get:OutputDirectory
    @get:Optional
    val destinationDir: File?
        get() = kotlinPluginVersion?.let { InspectionPlugin.kotlinPluginDirectory(it) }

    @Suppress("unused")
    @TaskAction
    fun apply() {
        if (kotlinPluginVersion == null) {
            logger.info("InspectionPlugin: Using kotlin plugin inherit from idea. No unzipping needed.")
            return
        }
        val unzip = Unzip(project)
        val copy = Copy(project)
        val unpacker = Unpacker(logger, unzip, copy)
        unpacker.unpack(sourceFile!!, destinationDir!!)
    }

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!
}