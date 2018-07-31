package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.InspectionPluginExtension
import java.io.File

open class DownloadKotlinPluginTask : AbstractDownloadTask() {

    private val extension: InspectionPluginExtension
        get() = project.extensions.getByName(InspectionPlugin.SHORT_NAME) as InspectionPluginExtension

    @get:Input
    override val url: String
        get() = InspectionPlugin.kotlinPluginLocation(extension.kotlinPluginVersion)

    @get:OutputFile
    override val destination: File
        get() = InspectionPlugin.kotlinPluginSource(extension.kotlinPluginVersion)

    @TaskAction
    fun apply() = download()
}