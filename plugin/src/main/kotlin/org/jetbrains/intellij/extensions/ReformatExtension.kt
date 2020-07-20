package org.jetbrains.intellij.extensions

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class ReformatExtension {

    /**
     * Quick fix are to be executed if found fixable code style errors.
     */
    @get:Input
    @get:Optional
    var quickFix: Boolean? = null

    /**
     * Needed for supporting configurations defined in groovy style
     */
    @Suppress("unused")
    fun quickFix(value: Boolean) {
        quickFix = value
    }
}
