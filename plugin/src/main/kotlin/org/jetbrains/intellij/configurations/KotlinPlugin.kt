package org.jetbrains.intellij.configurations

private val LONG_KOTLIN_PLUGIN_VERSION_PATTERN = """(\d{1,2}\.\d{1,2}\.\d{1,2})-release-(.+)""".toRegex()

private val SHORT_KOTLIN_PLUGIN_VERSION_PATTERN = """(\d{1,2}\.\d{1,2}\.\d{1,2})""".toRegex()

private fun isLongVersion(pluginVersion: String): Boolean = LONG_KOTLIN_PLUGIN_VERSION_PATTERN.matches(pluginVersion)

private fun isShortVersion(pluginVersion: String): Boolean = SHORT_KOTLIN_PLUGIN_VERSION_PATTERN.matches(pluginVersion)

private fun destructLongVersion(pluginVersion: String): Pair<String, String>? {
    val entire = LONG_KOTLIN_PLUGIN_VERSION_PATTERN.matchEntire(pluginVersion) ?: return null
    val (shortVersion, platformVersion) = entire.destructured
    val unifiedPlatformVersion = getUnifiedPlatformVersion(platformVersion)
    return Pair(shortVersion, unifiedPlatformVersion)
}

fun getUrl(pluginVersion: String, ideaArtifactVersion: String): String? = when {
    isShortVersion(pluginVersion) -> getUrlForShortVersion(pluginVersion, ideaArtifactVersion)
    isLongVersion(pluginVersion) -> getUrlForLongVersion(pluginVersion)
    else -> null
}

// Transforms e.g. IJ2018.2-1 to IJ2018.2
private fun getUnifiedPlatformVersion(platformVersion: String): String = platformVersion.substringBeforeLast("-")

// Transforms e.g. ideaIC:2018.2.4 to IJ2018.2.4
private fun getPlatformVersion(ideaArtifactVersion: String): String? {
    return when {
        ideaArtifactVersion.startsWith("ideaIC:") || ideaArtifactVersion.startsWith("ideaIU:") ->
            return "IJ" + ideaArtifactVersion.substringAfter(":")
        else -> null
    }
}

private fun getUrlForLongVersion(longVersion: String): String? {
    val (shortVersion, platformVersion) = destructLongVersion(longVersion) ?: return null
    return getUrlForShortVersionWithPlatformVersion(shortVersion, platformVersion)
}

private fun getUrlForShortVersion(shortVersion: String, ideaArtifactVersion: String): String? {
    val platformVersion = getPlatformVersion(ideaArtifactVersion) ?: return null
    return getUrlForShortVersionWithPlatformVersion(shortVersion, platformVersion)
}

private fun getUrlForShortVersionWithPlatformVersion(shortVersion: String, platformVersion: String) =
        getId(shortVersion, platformVersion)?.let {
            "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=$it"
        }

private fun getId(shortVersion: String, platformVersion: String): Int? {
    val dotCount = platformVersion.count { it == '.' }
    val majorVersion = when (dotCount) {
        2 -> platformVersion.substringBeforeLast(".")
        1 -> platformVersion
        else -> return null
    }
    return when (majorVersion) {
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
            "1.3.11" -> 52911
            "1.3.10" -> 52008
            "1.3.0" -> 51533
            "1.2.71" -> 50253
            "1.2.70" -> 49919
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
            "1.3.31" -> 61404
            "1.3.30" -> 60765
            "1.3.21" -> 57875
            "1.3.20" -> 54390
            "1.3.11" -> 52912
            "1.3.10" -> 52009
            "1.3.0" -> 51534
            "1.2.71" -> 50254
            "1.2.70" -> 49920
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
            "1.3.41" -> 65064
            "1.3.40" -> 64315
            "1.3.31" -> 61405
            "1.3.30" -> 60766
            "1.3.21" -> 57876
            "1.3.20" -> 54391
            "1.3.11" -> 52913
            "1.3.10" -> 52010
            "1.3.0" -> 51536
            "1.2.71" -> 50255
            "1.2.70" -> 49921
            "1.2.61" -> 49055
            "1.2.60" -> 48411
            "1.2.51" -> 47479
            "1.2.50" -> 46833
            "1.2.41" -> 45424
            "1.2.40" -> 45192
            else -> null
        }
        "IJ2018.3" -> when (shortVersion) {
            "1.3.41" -> 65065
            "1.3.40" -> 64316
            "1.3.31" -> 61406
            "1.3.30" -> 60767
            "1.3.21" -> 57877
            "1.3.20" -> 54392
            "1.3.11" -> 52914
            "1.3.10" -> 52011
            "1.3.0" -> 51537
            "1.2.71" -> 50256
            "1.2.70" -> 49922
            else -> null
        }
        "IJ2019.1" -> when (shortVersion) {
            "1.3.41" -> 65066
            "1.3.40" -> 64317
            "1.3.31" -> 61407
            "1.3.30" -> 60768
            "1.3.21" -> 60307
            else -> null
        }
        else -> null
    }
}
