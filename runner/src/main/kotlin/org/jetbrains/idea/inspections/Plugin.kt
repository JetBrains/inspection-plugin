package org.jetbrains.idea.inspections

import java.io.File

abstract class Plugin(val name: String, val directory: File) {
    data class PluginDescriptor(val version: String, val sinceBuild: String, val untilBuild: String)
    data class IdeaDescriptor(val version: String, val buildNumber: String)

    abstract fun isIncompatible(plugin: PluginDescriptor, idea: IdeaDescriptor): Boolean
}