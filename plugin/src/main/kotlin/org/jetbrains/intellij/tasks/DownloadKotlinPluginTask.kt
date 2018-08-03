package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class DownloadKotlinPluginTask : AbstractDownloadTask() {

    /**
     * Version of IDEA.
     */
    @get:Input
    val ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(extension.ideaVersion)

    /**
     * Version of IDEA Kotlin Plugin.
     */
    @get:Input
    val kotlinPluginVersion: KotlinPluginVersion
        get() = InspectionPlugin.kotlinPluginVersion(ideaVersion, extension.kotlinPluginVersion, extension.kotlinPluginLocation)

    @get:Input
    override val url: String
        get() = kotlinPluginVersion.location

    @get:OutputFile
    override val destination: File
        get() = InspectionPlugin.kotlinPluginSource(kotlinPluginVersion)

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!

    @TaskAction
    fun apply() = download()
}