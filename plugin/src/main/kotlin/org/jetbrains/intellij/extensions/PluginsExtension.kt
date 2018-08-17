package org.jetbrains.intellij.extensions

import org.gradle.api.Action

class PluginsExtension {

    /**
     * Configurations of kotlin plugin.
     */
    val kotlin = PluginExtension()

    @Suppress("unused")
    fun kotlin(action: Action<PluginExtension>) {
        action.execute(kotlin)
    }
}