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
        val idea = project.configurations.getByName("idea")
        logger.debug("Unzip 2")


        val markerFile = File(cacheDirectory, "markerFile")
        if (!markerFile.exists()) {
            logger.debug("Unzip 3")
            if (cacheDirectory.exists()) {
                cacheDirectory.deleteRecursively()
            }
            cacheDirectory.mkdir()
            logger.debug("Unzip 4")
            project.copy {
                it.from(project.zipTree(idea.singleFile))
                it.into(cacheDirectory)
            }
            logger.debug("Unzip 5")
            markerFile.createNewFile()
        }
        logger.debug("Unzip 10")
    }
}