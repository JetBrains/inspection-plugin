package org.jetbrains.intellij.utils

import org.gradle.api.logging.Logger
import org.jetbrains.intellij.InspectionPlugin
import java.io.File

class Unpacker(private val logger: Logger, private val unpack: Unpack, private val copy: Copy) {

    fun unpack(archive: File, destination: File) {
        logger.info("InspectionPlugin: Unzip task started, checking marker file $markerFile")
        if (isStored(archive)) {
            logger.info("InspectionPlugin: Archive $archive already unzipped")
            return
        }
        logger.warn("InspectionPlugin: Unzipping from $archive to $destination")
        if (destination.exists())
            destination.deleteRecursively()
        destination.mkdirs()
        copy(unpack(archive), destination)
        store(archive)
        logger.info("InspectionPlugin: Unzipping finished")
    }

    private val markerFile: File
        get() = File(InspectionPlugin.BASE_CACHE_DIRECTORY, "unzip.cache")

    private val File.globalIdentifier: String
        get() = name

    private fun isStored(archive: File) = markerFile.let { marker ->
        marker.exists() && marker.readLines().find { it == archive.globalIdentifier } != null
    }

    private fun store(archive: File) = markerFile.let {
        it.apply { if (!exists()) createNewFile() }
        logger.warn("InspectionPlugin: Marker file updated at $it")
        it.appendText(archive.globalIdentifier + '\n')
    }
}
