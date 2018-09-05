package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.intellij.configurations.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.utils.*
import org.jetbrains.intellij.utils.Copy
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipIdeaTask : ConventionTask() {
    @get:Input
    val ideaVersion: String
        get() = ideaVersion(extension.idea.version)

    @get:InputFile
    val archive: File
        get() = project.configurations.getByName(SHORT_NAME).singleFile

    @get:OutputDirectory
    val idea: File
        get() = ideaDirectory(ideaVersion)

    @get:OutputDirectory
    val kotlinPlugin: File
        get() = bundledKotlinPluginDirectory(ideaVersion)

    @Suppress("unused")
    @TaskAction
    fun apply() {
        UpToDateChecker(ideaVersion).apply {
            onUpToDate {
                logger.info("InspectionPlugin: No unzipping needed.")
            }
            onNonActual {
                val unzip = Unzip(project)
                val copy = Copy(project)
                val unpacker = Unpacker(logger, unzip, copy)
                unpacker.unpack(archive, idea)
                val ideaKotlinPlugin = File(idea, "plugins/Kotlin")
                if (ideaKotlinPlugin.exists()) {
                    copy(ideaKotlinPlugin, kotlinPlugin)
                    ideaKotlinPlugin.deleteRecursively()
                }
            }
        }
    }

    @get:Internal
    private val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!
}