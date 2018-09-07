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

private const val DEFAULT_IDEA_VERSION = "ideaIC:2018.2"

const val REFORMAT_SHORT_TASK_NAME = "reformat"

private val TEMP_DIRECTORY_IN_TEMP = File(System.getProperty("java.io.tmpdir"))

private val TEMP_DIRECTORY_IN_HOME = File(System.getProperty("user.home"))

private fun tempDirectory(inHome: Boolean) = if (inHome) TEMP_DIRECTORY_IN_HOME else TEMP_DIRECTORY_IN_TEMP

private fun baseCacheDirectory(inHome: Boolean) = File(tempDirectory(inHome), "inspection-plugin")

// Base directory for unzipped IDEA & plugins
private fun baseUnzippedDependenciesDirectory(inHome: Boolean) = File(baseCacheDirectory(inHome), "dependencies")

fun markersDirectory(inHome: Boolean) = File(baseCacheDirectory(inHome), "markers")

private val String.normalizedVersion: String
    get() = replace(':', '_').replace('.', '_')

fun ideaVersion(ideaVersion: String?) = ideaVersion ?: DEFAULT_IDEA_VERSION

fun PluginExtension.kotlinPluginLocation(ideaVersion: String): String? {
    if (location != null) return location
    if (version == null) return null
    return getUrl(version!!, ideaVersion)
}

// Used to store unzipped IDEA. Version-dependent.
fun ideaDirectory(version: String, inHome: Boolean): File {
    return File(baseUnzippedDependenciesDirectory(inHome), version.normalizedVersion)
}

// Used to store IDEA caches etc. Version-dependent.
// NB: this directory is always located in home directory, as regular IDEA launch does
fun ideaSystemDirectory(version: String): File {
    return File(baseCacheDirectory(inHome = true), "system_" + version.normalizedVersion)
}

// Used to download plugins
private fun pluginDownloadDirectory(inHome: Boolean) = File(baseCacheDirectory(inHome), "downloads")

fun kotlinPluginDownloadDirectory(location: String, tempInHome: Boolean): File {
    val hash = HashUtil.createCompactMD5(location)
    val name = "kotlin-plugin-$hash"
    return File(pluginDownloadDirectory(tempInHome), name)
}

// Used to store bundled Kotlin plugin
fun bundledKotlinPluginDirectory(ideaVersion: String, inHome: Boolean): File {
    val normIdeaVersion = ideaVersion.normalizedVersion
    val name = "kotlin-plugin-bundled-$normIdeaVersion"
    return File(baseUnzippedDependenciesDirectory(inHome), "$name/Kotlin")
}

// Used to store downloaded Kotlin plugin
fun PluginExtension.kotlinPluginDirectory(ideaVersion: String, inHome: Boolean): File {
    val name = if (location != null) {
        val hash = HashUtil.createCompactMD5(location)
        "kotlin-plugin-$hash"
    } else {
        val normIdeaVersion = ideaVersion.normalizedVersion
        val normKotlinPluginVersion = version?.normalizedVersion ?: "bundled"
        "kotlin-plugin-$normKotlinPluginVersion-$normIdeaVersion"
    }
    return File(baseUnzippedDependenciesDirectory(inHome), "$name/Kotlin")
}

