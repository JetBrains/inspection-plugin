package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.InspectionPlugin.Companion.BASE_CACHE_DIRECTORY
import org.jetbrains.intellij.InspectionPlugin.Companion.DEPENDENCY_SOURCE_DIRECTORY
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import org.jetbrains.intellij.versions.ToolVersion
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipKotlinPluginTask : AbstractUnzipTask() {

    /**
     * Tool (IDEA) version to use
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

    @get:InputFile
    override val sourceFile: File
        get() = InspectionPlugin.kotlinPluginSource(kotlinPluginVersion)

    @get:OutputDirectory
    override val destinationDir: File
        get() = InspectionPlugin.kotlinPluginDirectory(kotlinPluginVersion)

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!

    @TaskAction
    fun apply() = unzip()
}