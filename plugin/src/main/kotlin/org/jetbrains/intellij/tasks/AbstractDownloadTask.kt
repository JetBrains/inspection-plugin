package org.jetbrains.intellij.tasks

import org.gradle.api.internal.ConventionTask
import org.apache.http.HttpHeaders.USER_AGENT
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
//import org.apache.http.impl.client.HttpClientBuilder
//import org.gradle.internal.impldep.org.apache.http.client.methods.HttpGet
//import org.gradle.internal.impldep.org.apache.http.impl.client.HttpClientBuilder
import java.io.File

abstract class AbstractDownloadTask : ConventionTask() {

    abstract val url: String

    abstract val destination: File

    private fun upToDate() = destination.exists() && destination.isFile

    fun download() {
        if (upToDate()) {
            logger.info("No downloading needed for ${destination.name}")
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
    }
}