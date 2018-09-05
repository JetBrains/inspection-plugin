package org.jetbrains.intellij.utils

import org.gradle.api.logging.Logger
import java.io.File

class Unpacker(private val logger: Logger, private val unpack: Unpack, private val copy: Copy) {

    fun unpack(archive: File, destination: File) {
        logger.warn("InspectionPlugin: Unpacking from $archive to $destination")
        destination.deleteRecursively()
        destination.mkdirs()
        copy(unpack(archive), destination)
        logger.info("InspectionPlugin: Unpacking finished")
    }
}
