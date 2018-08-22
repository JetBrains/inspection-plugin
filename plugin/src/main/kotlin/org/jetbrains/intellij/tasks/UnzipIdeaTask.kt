package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.utils.Copy
import org.jetbrains.intellij.utils.Unpacker
import org.jetbrains.intellij.utils.Unzip
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipIdeaTask : ConventionTask() {
    @get:Input
    val ideaVersion: String
        get() = InspectionPlugin.ideaVersion(extension.idea.version)

    @get:InputFile
    val archive: File
        get() = project.configurations.getByName(InspectionPlugin.SHORT_NAME).singleFile

    @get:OutputDirectory
    val idea: File
        get() = InspectionPlugin.ideaDirectory(ideaVersion)

    @get:OutputDirectory
    val kotlinPlugin: File
        get() = InspectionPlugin.kotlinPluginDirectory(null, ideaVersion)

    @Suppress("unused")
    @TaskAction
    fun apply() {
        val unzip = Unzip(project)
        val copy = Copy(project)
        val unpacker = Unpacker(logger, unzip, copy)
        if (!unpacker.unpack(archive, idea)) return
        val ideaKotlinPlugin = File(idea, "plugins/Kotlin")
        if (ideaKotlinPlugin.exists()) {
            copy(ideaKotlinPlugin, kotlinPlugin)
            ideaKotlinPlugin.deleteRecursively()
        }
    }

    @get:Internal
    private val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!
}