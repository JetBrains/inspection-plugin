package org.jetbrains.intellij.extensions

class PluginExtension {

    /**
     * Version of plugin
     */
    var version: String? = null

    /**
     * Location of plugin
     */
    var location: String? = null

    /**
     * Needed for supporting configurations defined in groovy style
     */
    @Suppress("unused")
    fun version(value: String) {
        version = value
    }

    /**
     * Needed for supporting configurations defined in groovy style
     */
    @Suppress("unused")
    fun location(value: String) {
        location = value
    }
}
