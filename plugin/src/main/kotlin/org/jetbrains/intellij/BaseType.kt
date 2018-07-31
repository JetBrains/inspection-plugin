package org.jetbrains.intellij

sealed class BaseType(val value: String) {

    object Main : BaseType("Main")

    object Test : BaseType("Test")

    class Other(value: String) : BaseType(value)

    override fun toString() = value

    companion object {
        operator fun invoke(baseName: String) = when (baseName) {
            "main", "Main" -> Main
            "test", "Test" -> Test
            else -> Other(baseName)
        }
    }
}