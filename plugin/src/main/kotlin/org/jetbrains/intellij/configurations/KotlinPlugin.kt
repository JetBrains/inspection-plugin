package org.jetbrains.intellij.configurations

import org.gradle.api.Task
import org.jetbrains.intellij.exception

private val LONG_VERSION_PATTERN = """(\d{1,2}\.\d{1,2}\.\d{1,2})-release-(.+)""".toRegex()

private val SHORT_VERSION_PATTERN = """(\d{1,2}\.\d{1,2}\.\d{1,2})""".toRegex()

private fun isLongVersion(version: String): Boolean = LONG_VERSION_PATTERN.matches(version)

private fun isShortVersion(version: String): Boolean = SHORT_VERSION_PATTERN.matches(version)

private fun destructVersion(version: String): Pair<String, String>? {
    val entire = LONG_VERSION_PATTERN.matchEntire(version) ?: return null
    val (shortVersion, platformVersion) = entire.destructured
    val unifiedPlatformVersion = getUnifiedPlatformVersion(platformVersion)
    return Pair(shortVersion, unifiedPlatformVersion)
}

@Suppress("UNUSED_VARIABLE")
private fun isCompatible(version: String, ideaVersion: String): Boolean {
    val (shortVersion, platformVersion) = destructVersion(version) ?: return true
    val ideaPlatformVersion = getPlatformVersion(ideaVersion)
    return ideaPlatformVersion == null || ideaPlatformVersion == platformVersion
}

fun checkCompatibility(task: Task, version: String, ideaVersion: String) {
    if (isCompatible(version, ideaVersion)) return
    exception(task, "Incompatible idea $ideaVersion and kotlin plugin $version")
}

fun getUrl(version: String, ideaVersion: String): String? = when {
    isShortVersion(version) -> getUrlForShortVersion(version, ideaVersion)
    isLongVersion(version) -> getUrlForLongVersion(version)
    else -> null
}

private fun getUnifiedPlatformVersion(platformVersion: String) = when (platformVersion) {
    "IJ2017.1-1" -> "IJ2017.1"
    "IJ2017.2-1" -> "IJ2017.2"
    "IJ2017.3-1" -> "IJ2017.3"
    "IJ2018.1-1" -> "IJ2018.1"
    "IJ2018.2-1" -> "IJ2018.2"
    else -> platformVersion
}

private fun getPlatformVersion(ideaVersion: String): String? = when (ideaVersion) {
    "ideaIC:2017.1" -> "IJ2017.1"
    "ideaIC:2017.2" -> "IJ2017.2"
    "ideaIC:2017.3" -> "IJ2017.3"
    "ideaIC:2018.1" -> "IJ2018.1"
    "ideaIC:2018.2" -> "IJ2018.2"
    else -> null
}

private fun getUrlForLongVersion(longVersion: String): String? {
    val (shortVersion, platformVersion) = destructVersion(longVersion) ?: return null
    return getUrlForShortVersionWithPlatformVersion(shortVersion, platformVersion)
}

private fun getUrlForShortVersion(shortVersion: String, ideaVersion: String): String? {
    val platformVersion = getPlatformVersion(ideaVersion) ?: return null
    return getUrlForShortVersionWithPlatformVersion(shortVersion, platformVersion)
}

private fun getUrlForShortVersionWithPlatformVersion(shortVersion: String, platformVersion: String) =
        getId(shortVersion, platformVersion)?.let {
            "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=$it"
        }

private fun getId(shortVersion: String, platformVersion: String) = when (platformVersion) {
    "IJ2017.1" -> when (shortVersion) {
        "1.2.41" -> 45420
        "1.2.40" -> 45188
        "1.2.31" -> 44359
        "1.2.30" -> 43771
        "1.2.21" -> 42499
        else -> null
    }
    "IJ2017.2" -> when (shortVersion) {
        "1.2.61" -> 49052
        "1.2.60" -> 48408
        "1.2.51" -> 47476
        "1.2.50" -> 46830
        "1.2.41" -> 45421
        "1.2.40" -> 45189
        "1.2.31" -> 44360
        "1.2.30" -> 43773
        "1.2.21" -> 42500
        else -> null
    }
    "IJ2017.3" -> when (shortVersion) {
        "1.2.61" -> 49053
        "1.2.60" -> 48409
        "1.2.51" -> 47477
        "1.2.50" -> 46831
        "1.2.41" -> 45422
        "1.2.40" -> 45190
        "1.2.31" -> 44361
        "1.2.30" -> 43774
        "1.2.21" -> 42501
        else -> null
    }
    "IJ2018.1" -> when (shortVersion) {
        "1.2.61" -> 49054
        "1.2.60" -> 48410
        "1.2.51" -> 47478
        "1.2.50" -> 46832
        "1.2.41" -> 45423
        "1.2.40" -> 45191
        "1.2.31" -> 44362
        "1.2.30" -> 43775
        "1.2.21" -> 42502
        else -> null
    }
    "IJ2018.2" -> when (shortVersion) {
        "1.2.61" -> 49055
        "1.2.60" -> 48411
        "1.2.51" -> 47479
        "1.2.50" -> 46833
        "1.2.41" -> 45424
        "1.2.40" -> 45192
        else -> null
    }
    else -> null
}
