package org.jetbrains.intellij.versions

enum class KotlinPluginVersion(val value: String) {
    RELEASE_STUDIO_1_2_51__3_2_1("1.2.51-release-Studio3.2-1");

    override fun toString() = value

    companion object {
        operator fun invoke(value: String) = KotlinPluginVersion.values().find { it.value == value }
                ?: throw IllegalArgumentException("Unsupported kotlin plugin version version '$value'")
    }
}
