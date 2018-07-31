package org.jetbrains.intellij.extensions

class ReformatExtension {

    /**
     * Whether rule violations are to be displayed on the console.
     */
    var isQuiet: Boolean? = null

    /**
     * Quick fix are to be executed if found fixable code style errors.
     */
    var quickFix: Boolean? = null
}
