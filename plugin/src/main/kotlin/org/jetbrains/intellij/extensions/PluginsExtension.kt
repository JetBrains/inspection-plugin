package org.jetbrains.intellij.extensions

import org.gradle.api.Action
import org.gradle.api.tasks.Nested

class PluginsExtension {

    /**
     * Configurations of kotlin plugin.
     */
    @Nested
    val kotlin = PluginExtension()

    @Suppress("unused")
    fun kotlin(action: Action<PluginExtension>) {
        action.execute(kotlin)
    }
}