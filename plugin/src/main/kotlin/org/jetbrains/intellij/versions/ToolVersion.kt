package org.jetbrains.intellij.versions

enum class ToolVersion(val value: String) {
    IP_0_1_4("0.1.4");

    override fun toString() = value

    companion object {
        operator fun invoke(value: String) = ToolVersion.values().find { it.value == value }
                ?: throw IllegalArgumentException("Unsupported tool version version '$value'")
    }
}