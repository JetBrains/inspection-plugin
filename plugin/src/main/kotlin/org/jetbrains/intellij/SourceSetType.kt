package org.jetbrains.intellij

class SourceSetType(value: String) {

    val name = value.toLowerCase()

    val capitalize = value.capitalize()

    override fun toString() = name
}
