package org.jetbrains.intellij.tasks

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.InspectionPluginExtension
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipIdeaTask : AbstractUnzipTask() {
    private val configuration: Configuration
        get() = project.configurations.getByName(InspectionPlugin.SHORT_NAME)

    private val extension: InspectionPluginExtension
        get() = project.extensions.getByName(InspectionPlugin.SHORT_NAME) as InspectionPluginExtension

    @get:InputFile
    override val sourceFile: File
        get() = configuration.singleFile

    @get:OutputDirectory
    override val destinationDir: File
        get() = InspectionPlugin.ideaDirectory(extension.toolVersion)

    @get:OutputFile
    override val markerFile: File
        get() = File(InspectionPlugin.DEPENDENCY_SOURCE_DIRECTORY, "markerFile")

    @Suppress("unused")
    @TaskAction
    fun apply() = unzip()
}