package org.jetbrains.intellij.extensions

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class PluginExtension {

    /**
     * Version of plugin
     */
    @get:Input
    @get:Optional
    var version: String? = null

    /**
     * Location of plugin
     */
    @get:Input
    @get:Optional
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
