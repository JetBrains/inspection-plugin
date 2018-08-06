package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.utils.Copy
import org.jetbrains.intellij.utils.Unpacker
import org.jetbrains.intellij.utils.Unzip
import org.jetbrains.intellij.versions.IdeaVersion
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipIdeaTask : ConventionTask() {
    @get:Input
    val ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(extension.ideaVersion)

    @get:InputFile
    val sourceFile: File
        get() = project.configurations.getByName(InspectionPlugin.SHORT_NAME).singleFile

    @get:OutputDirectory
    val destinationDir: File
        get() = InspectionPlugin.ideaDirectory(ideaVersion)

    @Suppress("unused")
    @TaskAction
    fun apply() {
        val unzip = Unzip(project)
        val copy = Copy(project)
        val unpacker = Unpacker(logger, unzip, copy)
        unpacker.unpack(sourceFile, destinationDir)
    }

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!
}