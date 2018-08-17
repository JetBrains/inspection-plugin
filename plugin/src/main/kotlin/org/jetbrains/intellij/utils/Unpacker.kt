package org.jetbrains.intellij.utils

import org.gradle.api.logging.Logger
import org.gradle.internal.hash.HashUtil
import org.jetbrains.intellij.InspectionPlugin
import java.io.File

class Unpacker(private val logger: Logger, private val unpack: Unpack, private val copy: Copy) {

    fun unpack(archive: File, destination: File): Boolean {
        val markerFile = getMarkerFile(archive)
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

    private fun getMarkerFile(archive: File): File {
        val hash = HashUtil.sha256(archive).asHexString()
        return File(InspectionPlugin.MARKERS_DIRECTORY, "unpack-$hash.marker")
    }
}
