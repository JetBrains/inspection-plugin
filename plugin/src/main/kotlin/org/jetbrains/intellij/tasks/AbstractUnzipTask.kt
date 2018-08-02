package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.internal.hash.HashUtil.sha256
import org.jetbrains.intellij.InspectionPlugin
import java.io.File

abstract class AbstractUnzipTask : ConventionTask() {

    abstract val sourceFile: File

    abstract val destinationDir: File

    private val markerFile: File
        get() = File(InspectionPlugin.DEPENDENCY_SOURCE_DIRECTORY, "markerFile.txt")

    private val File.hash: String
        get() = sha256(this).asHexString()

    private fun isStored(archive: File) = markerFile.let {
        it.exists() && it.readLines().find { it == archive.hash } != null
    }

    private fun store(archive: File) = markerFile.let {
        it.apply { if (!exists()) createNewFile() }
        logger.warn("Marker file updated at: $it")
        it.appendText(archive.hash + '\n')
    }

    protected fun unzip() {
        logger.info("Unzip task started, checking marker file $markerFile")
        val archive = sourceFile
        val destination = destinationDir
        if (isStored(archive)) {
            logger.info("Archive $archive already unzipped")
            return
        }
        logger.warn("Unzipping from $archive to $destination")
        if (destination.exists())
            destination.deleteRecursively()
        destination.mkdirs()
        project.copy { copy ->
            copy.from(project.zipTree(archive))
            copy.into(destination)
        }
        store(archive)
        logger.info("Unzipping finished")
    }
}
