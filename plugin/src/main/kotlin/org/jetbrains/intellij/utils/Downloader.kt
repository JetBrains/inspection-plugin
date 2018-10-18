package org.jetbrains.intellij.utils

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.gradle.api.logging.Logger
import org.gradle.internal.hash.HashUtil
import org.jetbrains.intellij.configurations.markersDirectory
import java.io.File

class Downloader(private val logger: Logger) {

    fun download(url: String, intoHome: Boolean, destinationDirectory: File) {
        val markerFile = getMarkerFile(url, intoHome)
        if (markerFile.exists()) {
            logger.info("InspectionPlugin: No downloading needed.")
            return
        }
        val client = DefaultHttpClient()
        val request = HttpGet(url)
        request.addHeader("User-Agent", "User-Agent")
        val response = client.execute(request)
        destinationDirectory.deleteRecursively()
        destinationDirectory.mkdirs()
        val destination = File(destinationDirectory, "archive.zip")
        response.entity.content.use { input ->
            destination.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }
        markerFile.parentFile.mkdirs()
        markerFile.createNewFile()
    }

    private fun getMarkerFile(url: String, inHome: Boolean): File {
        val hash = HashUtil.createCompactMD5(url)
        return File(markersDirectory(inHome), "download-$hash.marker")
    }
}