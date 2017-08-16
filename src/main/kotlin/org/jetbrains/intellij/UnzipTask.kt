package org.jetbrains.intellij

import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import java.io.File

open class UnzipTask : Sync() {

    companion object {
        val cacheDirectory = File("lib/idea")
    }

    @TaskAction
    fun unzip() {
        logger.info("Unzipping IDEA")
        val inspections = project.configurations.getByName(InspectionPlugin.SHORT_NAME)
        logger.debug("Unzip 2")

        val extension = project.extensions.getByName(InspectionPlugin.SHORT_NAME) as InspectionPluginExtension
        val ideaVersion = extension.ideaVersion

        val markerFile = File(cacheDirectory, "markerFile")
        if (markerFile.exists()) {
            val line = markerFile.readLines().firstOrNull()
            if (line == ideaVersion) {
                logger.debug("No unzipping needed")
                return
            }
        }
        logger.debug("Unzip 3")
        if (cacheDirectory.exists()) {
            cacheDirectory.deleteRecursively()
        }
        cacheDirectory.mkdir()
        logger.debug("Unzip 4")
        project.copy {
            it.from(project.zipTree(inspections.singleFile))
            it.into(cacheDirectory)
        }
        logger.debug("Unzip 5")
        val writer = markerFile.bufferedWriter()
        writer.append(ideaVersion)
        logger.debug("Unzip 10")
    }
}