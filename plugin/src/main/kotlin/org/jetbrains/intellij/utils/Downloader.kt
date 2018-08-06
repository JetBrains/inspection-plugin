package org.jetbrains.intellij.utils

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.gradle.api.logging.Logger
import org.jetbrains.intellij.InspectionPlugin
import java.io.File

class Downloader(private val logger: Logger) {

    fun download(url: String, destination: File) {
        if (isStored(url)) {
            logger.info("InspectionPlugin: No downloading needed for ${destination.name}")
            return
        }
        val client = DefaultHttpClient()
        val request = HttpGet(url)
        request.addHeader("User-Agent", "User-Agent")
        val response = client.execute(request)
        destination.parentFile.mkdirs()
        response.entity.content.use { input ->
            destination.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }
        store(url)
    }

    private val markerFile: File
        get() = File(InspectionPlugin.BASE_CACHE_DIRECTORY, "download.cache")

    private val String.globalIdentifier: String
        get() = this

    private fun isStored(url: String) = markerFile.let { marker ->
        marker.exists() && marker.readLines().find { it == url.globalIdentifier } != null
    }

    private fun store(url: String) = markerFile.let {
        it.apply { if (!exists()) createNewFile() }
        logger.warn("InspectionPlugin: Marker file updated at $it")
        it.appendText(url.globalIdentifier + '\n')
    }
}