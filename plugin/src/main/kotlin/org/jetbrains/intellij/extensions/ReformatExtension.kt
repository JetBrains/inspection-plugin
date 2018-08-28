package org.jetbrains.intellij.extensions

class ReformatExtension {

    /**
     * Quick fix are to be executed if found fixable code style errors.
     */
    var quickFix: Boolean? = null

    /**
     * Needed for supporting configurations defined in groovy style
     */
    @Suppress("unused")
    fun quickFix(value: Boolean) {
        quickFix = value
    }
}
