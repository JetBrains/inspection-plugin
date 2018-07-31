package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.versions.KotlinPluginVersion
import org.jetbrains.intellij.versions.ToolVersion
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class DownloadKotlinPluginTask : AbstractDownloadTask() {

    /**
     * Tool (IDEA) version to use
     */
    @get:Input
    val toolVersion: ToolVersion
        get() = InspectionPlugin.toolVersion(extension.toolVersion)

    /**
     * Version of IDEA Kotlin Plugin.
     */
    @get:Input
    val kotlinPluginVersion: KotlinPluginVersion
        get() = InspectionPlugin.kotlinPluginVersion(toolVersion, extension.kotlinPluginVersion)

    @get:Input
    override val url: String
        get() = InspectionPlugin.kotlinPluginLocation(kotlinPluginVersion)

    @get:OutputFile
    override val destination: File
        get() = InspectionPlugin.kotlinPluginSource(kotlinPluginVersion)

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!

    @TaskAction
    fun apply() = download()
}