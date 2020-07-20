package org.jetbrains.intellij.extensions

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class IdeaExtension {

    /**
     * Version of IDEA
     */
    @Suppress("unused")
    @Input
    @Optional
    var version: String? = null

    /**
     * Needed for supporting configurations defined in groovy style
     */
    @Suppress("unused")
    fun version(value: String) {
        version = value
    }
}
