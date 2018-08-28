package org.jetbrains.intellij.extensions

class IdeaExtension {

    /**
     * Version of IDEA
     */
    @Suppress("unused")
    var version: String? = null

    /**
     * Needed for supporting configurations defined in groovy style
     */
    @Suppress("unused")
    fun version(value: String) {
        version = value
    }
}
