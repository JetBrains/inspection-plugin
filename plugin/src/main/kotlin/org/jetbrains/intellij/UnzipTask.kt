package org.jetbrains.intellij

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class UnzipTask : ConventionTask() {

    companion object {
        private val baseCacheDirectory = run {
            val tempDir = File(System.getProperty("java.io.tmpdir"))
            File(tempDir, "inspection-plugin/idea")
        }

        private val Project.ideaVersion: String
            get() = (project.extensions.getByName(InspectionPlugin.SHORT_NAME) as InspectionPluginExtension).toolVersion

        val Project.cacheDirectory: File
            get() = File(baseCacheDirectory,
                    ideaVersion.replace(':', '_').replace('.', '_'))

        val Project.buildNumber: String
            get() = File(cacheDirectory, "build.txt").let {
                if (it.exists()) {
                    it.readText().dropWhile { !it.isDigit() }.let {
                        if (it.isNotEmpty()) it else null
                    }
                } else {
                    null
                }
            } ?: "172.1" // default build number

        val Project.usesUltimate: Boolean
            get() = File(cacheDirectory, "build.txt").let {
                if (it.exists()) {
                    it.readText().startsWith("IU")
                } else {
                    false
                }
            }
    }

    private val inspections: Configuration
        get() = project.configurations.getByName(InspectionPlugin.SHORT_NAME)

    @get:InputFile
    val sourceFile: File
        get() = inspections.singleFile

    @get:Input
    val ideaVersion: String
        get() = project.ideaVersion

    @get:OutputDirectory
    val destinationDir: File
        get() = project.cacheDirectory

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