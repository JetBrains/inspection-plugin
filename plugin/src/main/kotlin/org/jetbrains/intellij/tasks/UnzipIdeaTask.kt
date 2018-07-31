package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.ToolVersion
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipIdeaTask : AbstractUnzipTask() {

    /**
     * Tool (IDEA) version to use
     */
    @get:Input
    val toolVersion: ToolVersion
        get() = InspectionPlugin.toolVersion(extension.toolVersion)

    /**
     * Version of IDEA.
     */
    @get:Input
    val ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(toolVersion, extension.ideaVersion)

    @get:InputFile
    override val sourceFile: File
        get() = project.configurations.getByName(InspectionPlugin.SHORT_NAME).singleFile

    @get:OutputDirectory
    override val destinationDir: File
        get() = InspectionPlugin.ideaDirectory(ideaVersion)

    @get:OutputFile
    override val markerFile: File
        get() = File(InspectionPlugin.DEPENDENCY_SOURCE_DIRECTORY, "markerFile")

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!

    @Suppress("unused")
    @TaskAction
    fun apply() = unzip()
}