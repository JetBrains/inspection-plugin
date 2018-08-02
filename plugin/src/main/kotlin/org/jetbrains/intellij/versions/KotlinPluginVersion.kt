package org.jetbrains.intellij.versions

enum class KotlinPluginVersion(val value: String) {
    RELEASE_IJ2017_2_1__1_2_60("1.2.60-release-IJ2017.2-1"),
    RELEASE_IJ2017_3_1__1_2_60("1.2.60-release-IJ2017.3-1"),
    RELEASE_IJ2018_1_1__1_2_60("1.2.60-release-IJ2018.1-1"),
    RELEASE_IJ2018_2_1__1_2_60("1.2.60-release-IJ2018.2-1");

    override fun toString() = value

    companion object {
        operator fun invoke(value: String) = KotlinPluginVersion.values().find { it.value == value }
                ?: throw IllegalArgumentException("Unsupported kotlin plugin version version '$value'")
    }
}
