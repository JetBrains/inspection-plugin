package org.jetbrains.intellij.configurations

import org.gradle.internal.hash.HashUtil
import org.jetbrains.intellij.extensions.PluginExtension
import java.io.File

const val SHORT_NAME = "inspections"

const val CHECK_TASK_NAME = "checkInspections"

const val UNZIP_IDEA_TASK_NAME = "unzip-idea"

const val DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME = "download-kotlin-plugin"

const val UNZIP_KOTLIN_PLUGIN_TASK_NAME = "unzip-kotlin-plugin"

const val IDEA_TASK_NAME = "idea"

const val DEFAULT_IDEA_VERSION = "ideaIC:2017.3"

const val REFORMAT_SHORT_TASK_NAME = "reformat"

val TEMP_DIRECTORY = File(System.getProperty("user.home"))

val BASE_CACHE_DIRECTORY = File(TEMP_DIRECTORY, ".GradleInspectionPluginCaches")

val MARKERS_DIRECTORY = File(BASE_CACHE_DIRECTORY, "markers")

val DEPENDENCY_SOURCE_DIRECTORY = File(BASE_CACHE_DIRECTORY, "dependencies")

val DOWNLOAD_DIRECTORY = File(BASE_CACHE_DIRECTORY, "downloads")

val IDEA_SYSTEM_DIRECTORY = File(BASE_CACHE_DIRECTORY, "system")

val LOCKS_DIRECTORY = File(BASE_CACHE_DIRECTORY, "locks")

private val String.normalizedVersion: String
    get() = replace(':', '_').replace('.', '_')

fun ideaVersion(ideaVersion: String?) = ideaVersion ?: DEFAULT_IDEA_VERSION

fun PluginExtension.kotlinPluginLocation(ideaVersion: String): String? {
    if (location != null) return location
    return version?.let { getUrl(it, ideaVersion) }
}

fun kotlinPluginArchiveDirectory(location: String?): File? {
    if (location == null) return null
    val hash = HashUtil.createCompactMD5(location)
    val name = "kotlin-plugin-$hash"
    return File(DOWNLOAD_DIRECTORY, name)
}

fun kotlinPluginArchive(location: String?): File? {
    return kotlinPluginArchiveDirectory(location)?.listFiles()?.firstOrNull()
}

fun bundledKotlinPluginDirectory(ideaVersion: String): File {
    val normIdeaVersion = ideaVersion.normalizedVersion
    val name = "kotlin-plugin-bundled-$normIdeaVersion"
    return File(DEPENDENCY_SOURCE_DIRECTORY, "$name/Kotlin")
}

fun PluginExtension.kotlinPluginDirectory(ideaVersion: String): File {
    val name = if (location != null) {
        val hash = HashUtil.createCompactMD5(location)
        "kotlin-plugin-$hash"
    } else {
        val normIdeaVersion = ideaVersion.normalizedVersion
        val normKotlinPluginVersion = version?.normalizedVersion ?: "bundled"
        "kotlin-plugin-$normKotlinPluginVersion-$normIdeaVersion"
    }
    return File(DEPENDENCY_SOURCE_DIRECTORY, "$name/Kotlin")
}

fun ideaDirectory(version: String) = File(DEPENDENCY_SOURCE_DIRECTORY, version.normalizedVersion)
