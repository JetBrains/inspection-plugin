package org.jetbrains.intellij

class BaseType(private val value: String) {

    val baseName: String
        get() = value.toLowerCase()

    val baseTitle: String
        get() = value.capitalize()

    override fun toString() = baseName
}