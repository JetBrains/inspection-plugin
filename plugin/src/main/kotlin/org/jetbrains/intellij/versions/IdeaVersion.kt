package org.jetbrains.intellij.versions

enum class IdeaVersion(val value: String) {
    IDEA_IC_2017_3("ideaIC:2017.3");

    override fun toString() = value

    companion object {
        operator fun invoke(value: String)= IdeaVersion.values().find { it.value == value }
                ?: throw IllegalArgumentException("Unsupported idea version version '$value'")
    }
}
