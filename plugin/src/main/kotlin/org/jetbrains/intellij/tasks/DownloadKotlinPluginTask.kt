package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.utils.Downloader
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class DownloadKotlinPluginTask : ConventionTask() {

    @get:Input
    val ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(extension.ideaVersion)

    @get:Input
    @get:Optional
    val kotlinPluginVersion: KotlinPluginVersion?
        get() = InspectionPlugin.kotlinPluginVersion(extension.kotlinPluginVersion, extension.kotlinPluginLocation)

    @get:Input
    @get:Optional
    val url: String?
        get() = kotlinPluginVersion?.location

    @get:OutputFile
    @get:Optional
    val destination: File?
        get() = kotlinPluginVersion?.let { InspectionPlugin.kotlinPluginSource(it) }

    @Suppress("unused")
    @TaskAction
    fun apply() {
        if (kotlinPluginVersion == null) {
            logger.info("InspectionPlugin: Using kotlin plugin inherit from idea. No downloading needed.")
            return
        }
        Downloader(logger).download(url!!, destination!!)
    }

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!
}