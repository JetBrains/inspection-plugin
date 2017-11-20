package org.jetbrains.intellij

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import java.io.File

open class UnzipTask : ConventionTask() {

    companion object {
        val cacheDirectory = run {
            val tempDir = File(System.getProperty("java.io.tmpdir"))
            File(tempDir, "inspection-plugin/idea")
        }

        fun buildNumber(): String? = File(cacheDirectory, "build.txt").let {
            if (it.exists()) {
                it.readText().dropWhile { !it.isDigit() }.let {
                    if (it.isNotEmpty()) it else null
                }
            }
            else {
                null
            }
        } ?: "172.1" // default build number
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
        logger.info("Unzip task started, checking marker file $markerFile")

        val markerFile = markerFile
        if (markerFile.exists()) {
            val line = markerFile.readLines().firstOrNull()
            if (line == ideaVersion) {
                logger.debug("No unzipping needed")
                return
            }
        }
        logger.warn("Unzipping IDEA from ${sourceFile.path} to ${destinationDir.path}")
        if (destinationDir.exists()) {
            destinationDir.deleteRecursively()
        }
        destinationDir.mkdir()
        project.copy {
            it.from(project.zipTree(sourceFile))
            it.into(destinationDir)
        }
        logger.debug("Unzipping finished")
        markerFile.bufferedWriter().use {
            it.append(ideaVersion)
        }
        logger.warn("IDEA marker file created at: " + markerFile.path)
    }
}