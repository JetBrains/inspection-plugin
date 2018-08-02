package org.jetbrains.intellij.versions

enum class ToolVersion(val value: String) {
    IDEA_IC_2017_2("ideaIC:2017.2"),
    IDEA_IC_2017_3("ideaIC:2017.3"),
    IDEA_IC_2018_1("ideaIC:2018.1"),
    IDEA_IC_2018_2("ideaIC:182.2574.2");

    override fun toString() = value

    companion object {
        operator fun invoke(value: String) = ToolVersion.values().find { it.value == value }
                ?: throw IllegalArgumentException("Unsupported tool version version '$value'")
    }
}