package org.jetbrains.intellij.configurations

import org.gradle.internal.hash.HashUtil
import java.io.File

const val SHORT_NAME = "inspections"

const val CHECK_TASK_NAME = "checkInspections"

const val UNZIP_IDEA_TASK_NAME = "unzip-idea"

const val DOWNLOAD_KOTLIN_PLUGIN_TASK_NAME = "download-kotlin-plugin"

const val UNZIP_KOTLIN_PLUGIN_TASK_NAME = "unzip-kotlin-plugin"

const val IDEA_TASK_NAME = "idea"

const val DEFAULT_IDEA_VERSION = "ideaIC:2017.3"

const val REFORMAT_SHORT_TASK_NAME = "reformat"

private val TEMP_DIRECTORY = File(System.getProperty("java.io.tmpdir"))

private val BASE_CACHE_DIRECTORY = File(TEMP_DIRECTORY, "inspection-plugin")

val MARKERS_DIRECTORY = File(BASE_CACHE_DIRECTORY, "markers")

val DEPENDENCY_SOURCE_DIRECTORY = File(BASE_CACHE_DIRECTORY, "dependencies")

val DOWNLOAD_DIRECTORY = File(BASE_CACHE_DIRECTORY, "downloads")

val IDEA_SYSTEM_DIRECTORY = File(BASE_CACHE_DIRECTORY, "system")

val LOCKS_DIRECTORY = File(BASE_CACHE_DIRECTORY, "locks")

private val String.normalizedVersion: String
    get() = replace(':', '_').replace('.', '_')

fun ideaVersion(ideaVersion: String?) = ideaVersion ?: DEFAULT_IDEA_VERSION

fun kotlinPluginLocation(version: String?, ideaVersion: String) =
        version?.let { getUrl(it, ideaVersion) }

fun kotlinPluginArchiveDirectory(location: String): File {
    val hash = HashUtil.createCompactMD5(location)
    val name = "kotlin-plugin-$hash"
    return File(DOWNLOAD_DIRECTORY, name)
}

fun kotlinPluginDirectory(kotlinPluginVersion: String?, ideaVersion: String): File {
    val normIdeaVersion = ideaVersion.normalizedVersion
    val normKotlinPluginVersion = kotlinPluginVersion.toString().normalizedVersion
    val name = "Kotlin-$normKotlinPluginVersion-$normIdeaVersion"
    return File(DEPENDENCY_SOURCE_DIRECTORY, "$name/Kotlin")
}

fun ideaDirectory(version: String) = File(DEPENDENCY_SOURCE_DIRECTORY, version.normalizedVersion)
