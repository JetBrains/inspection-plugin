package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.InspectionPlugin.Companion.BASE_CACHE_DIRECTORY
import org.jetbrains.intellij.InspectionPluginExtension
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipKotlinPluginTask : AbstractUnzipTask() {
    private val extension: InspectionPluginExtension
        get() = project.extensions.getByName(InspectionPlugin.SHORT_NAME) as InspectionPluginExtension

    @get:InputFile
    override val sourceFile: File
        get() = InspectionPlugin.kotlinPluginSource(extension.kotlinPluginVersion)

    @get:OutputDirectory
    override val destinationDir: File
        get() = InspectionPlugin.kotlinPluginDirectory(extension.kotlinPluginVersion)

    @get:OutputFile
    override val markerFile: File
        get() = File(BASE_CACHE_DIRECTORY, "markerFile")

    @Suppress("unused")
    @TaskAction
    fun apply() = unzip()
}