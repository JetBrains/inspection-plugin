package org.jetbrains.intellij.extensions

class InspectionExtension {

    /**
     * Quick fix are to be executed if found fixable errors.
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