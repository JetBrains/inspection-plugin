package org.jetbrains.intellij

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import java.io.File

open class UnzipTask : ConventionTask() {

    companion object {
        val cacheDirectory = File("lib/idea")
    }

    private val inspections: Configuration
        get() = project.configurations.getByName(InspectionPlugin.SHORT_NAME)

    @get:InputFile
    val sourceFile: File
        get() = inspections.singleFile

    @get:Input
    val ideaVersion: String
        get() = (project.extensions.getByName(InspectionPlugin.SHORT_NAME) as InspectionPluginExtension).ideaVersion

    @get:OutputDirectory
    val destinationDir: File
        get() = cacheDirectory

    @get:OutputFile
    val markerFile: File
        get() = File(destinationDir, "markerFile")

    @TaskAction
    fun unzip() {
        logger.info("Unzipping IDEA")
        logger.debug("Unzip 2")

        val markerFile = markerFile
        if (markerFile.exists()) {
            val line = markerFile.readLines().firstOrNull()
            if (line == ideaVersion) {
                logger.debug("No unzipping needed")
                return
            }
        }
        logger.debug("Unzip 3")
        if (destinationDir.exists()) {
            destinationDir.deleteRecursively()
        }
        destinationDir.mkdir()
        logger.debug("Unzip 4")
        project.copy {
            it.from(project.zipTree(sourceFile))
            it.into(destinationDir)
        }
        logger.debug("Unzip 5")
        val writer = markerFile.bufferedWriter()
        writer.append(ideaVersion)
        logger.debug("Unzip 10")
    }
}