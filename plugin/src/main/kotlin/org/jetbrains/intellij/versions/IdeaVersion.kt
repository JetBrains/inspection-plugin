package org.jetbrains.intellij.versions

@Suppress("ClassName")
sealed class IdeaVersion(val value: String): java.io.Serializable {
    object IDEA_IC_2017_2 : IdeaVersion(S_IDEA_IC_2017_2)
    object IDEA_IC_2017_3 : IdeaVersion(S_IDEA_IC_2017_3)
    object IDEA_IC_2018_1 : IdeaVersion(S_IDEA_IC_2018_1)
    object IDEA_IC_2018_2 : IdeaVersion(S_IDEA_IC_2018_2)
    class Other(version: String) : IdeaVersion(version)

    override fun toString() = value

    val mavenUrl: String
        get() = "com.jetbrains.intellij.idea:$value"

    companion object {
        private const val S_IDEA_IC_2017_2 = "ideaIC:2017.2"
        private const val S_IDEA_IC_2017_3 = "ideaIC:2017.3"
        private const val S_IDEA_IC_2018_1 = "ideaIC:2018.1"
        private const val S_IDEA_IC_2018_2 = "ideaIC:2018.2"

        operator fun invoke(value: String): IdeaVersion = when (value) {
            S_IDEA_IC_2017_2 -> IDEA_IC_2017_2
            S_IDEA_IC_2017_3 -> IDEA_IC_2017_3
            S_IDEA_IC_2018_1 -> IDEA_IC_2018_1
            S_IDEA_IC_2018_2 -> IDEA_IC_2018_2
            else -> Other(value)
        }
    }
}
