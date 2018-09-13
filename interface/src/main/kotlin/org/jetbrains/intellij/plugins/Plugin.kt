package org.jetbrains.intellij.plugins

import java.io.File

abstract class Plugin(val name: String, val directory: File) {

    /**
     * Checks plugin compatibility with idea
     *
     * @return the incompatible reason. Returns null if plugin and idea is compatible between self
     */
    abstract fun checkCompatibility(plugin: PluginDescriptor, idea: IdeaDescriptor): String?

    data class PluginDescriptor(val version: String, val sinceBuild: String, val untilBuild: String)

    data class IdeaDescriptor(val version: String, val buildNumber: String)
}