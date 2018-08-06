package org.jetbrains.intellij.versions

@Suppress("ClassName")
sealed class KotlinPluginVersion(val value: String, val location: String): java.io.Serializable {
    object RELEASE_IJ2017_2_1__1_2_60 : KotlinPluginVersion(S_RELEASE_IJ2017_2_1__1_2_60, getUrl(48408))
    object RELEASE_IJ2017_3_1__1_2_60 : KotlinPluginVersion(S_RELEASE_IJ2017_3_1__1_2_60, getUrl(48409))
    object RELEASE_IJ2018_1_1__1_2_60 : KotlinPluginVersion(S_RELEASE_IJ2018_1_1__1_2_60, getUrl(48410))
    object RELEASE_IJ2018_2_1__1_2_60 : KotlinPluginVersion(S_RELEASE_IJ2018_2_1__1_2_60, getUrl(48411))
    class Other(value: String, url: String) : KotlinPluginVersion(value, url)

    override fun toString() = value

    companion object {
        private const val S_RELEASE_IJ2017_2_1__1_2_60 = "1.2.60-release-IJ2017.2-1"
        private const val S_RELEASE_IJ2017_3_1__1_2_60 = "1.2.60-release-IJ2017.3-1"
        private const val S_RELEASE_IJ2018_1_1__1_2_60 = "1.2.60-release-IJ2018.1-1"
        private const val S_RELEASE_IJ2018_2_1__1_2_60 = "1.2.60-release-IJ2018.2-1"

        private fun getUrl(id: Int) = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=$id"

        private operator fun invoke(value: String): KotlinPluginVersion = when (value) {
            S_RELEASE_IJ2017_2_1__1_2_60 -> RELEASE_IJ2017_2_1__1_2_60
            S_RELEASE_IJ2017_3_1__1_2_60 -> RELEASE_IJ2017_3_1__1_2_60
            S_RELEASE_IJ2018_1_1__1_2_60 -> RELEASE_IJ2018_1_1__1_2_60
            S_RELEASE_IJ2018_2_1__1_2_60 -> RELEASE_IJ2018_2_1__1_2_60
            else -> throw IllegalArgumentException("Undefined kotlin plugin place for custom plugin version $value")
        }

        operator fun invoke(value: String, url: String?): KotlinPluginVersion =
                if (url == null) KotlinPluginVersion(value) else Other(value, url)
    }
}
