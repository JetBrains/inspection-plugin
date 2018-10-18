package org.jetbrains.intellij.utils

import org.gradle.api.logging.Logger
import org.gradle.internal.hash.HashUtil
import org.jetbrains.intellij.configurations.markersDirectory
import java.io.File

class Unpacker(private val logger: Logger, private val unpack: Unpack, private val copy: Copy) {

    fun unpack(archive: File, markersInHome: Boolean, destination: File): Boolean {
        val markerFile = getMarkerFile(archive, markersInHome)
        if (markerFile.exists()) {
            logger.info("InspectionPlugin: No unzipping needed.")
            return false
        }
        logger.warn("InspectionPlugin: Unzipping from $archive to $destination")
        destination.deleteRecursively()
        destination.mkdirs()
        copy(unpack(archive), destination)
        markerFile.parentFile.mkdirs()
        markerFile.createNewFile()
        logger.info("InspectionPlugin: Unzipping finished")
        return true
    }

    private fun getMarkerFile(archive: File, inHome: Boolean): File {
        val hash = HashUtil.sha256(archive).asHexString()
        return File(markersDirectory(inHome), "unpack-$hash.marker")
    }
}
