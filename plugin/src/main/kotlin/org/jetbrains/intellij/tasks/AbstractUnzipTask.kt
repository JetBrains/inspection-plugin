package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.internal.hash.HashUtil.sha256
import java.io.File

abstract class AbstractUnzipTask : ConventionTask() {

    abstract val sourceFile: File

    abstract val destinationDir: File

    abstract val markerFile: File

    private val File.hash: String
        get() = sha256(this).asHexString()

    private fun upToDate(archive: File) = markerFile.let {
        it.exists() && it.readLines().find { it == archive.hash } != null
    }

    private fun doUpToDate(archive: File) = markerFile.let {
        it.apply { if (!exists()) createNewFile() }
        logger.warn("Marker file created at: ${markerFile.path}")
        it.bufferedWriter().use {
            it.append(archive.hash)
        }
    }

    protected fun unzip() {
        logger.info("Unzip task started, checking marker file $markerFile")
        val archive = sourceFile
        if (upToDate(archive)) {
            logger.info("No unzipping needed for ${archive.name}")
            return
        }
        destinationDir.let {
            logger.warn("Unzipping from ${archive.path} to ${it.path}")
            if (it.exists())
                it.deleteRecursively()
            it.mkdirs()
            project.copy {
                it.from(project.zipTree(archive))
                it.into(it)
            }
        }
        logger.info("Unzipping finished")
        doUpToDate(archive)
    }
}
