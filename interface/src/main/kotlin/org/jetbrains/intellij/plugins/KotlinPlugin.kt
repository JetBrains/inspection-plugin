package org.jetbrains.intellij.plugins

import java.io.File

class KotlinPlugin(directory: File) : Plugin("Kotlin", directory) {
    override fun checkCompatibility(plugin: PluginDescriptor, idea: IdeaDescriptor): String? {
        if ("IJ" in plugin.version) return null
        return "Unsupported non idea kotlin plugins: ${plugin.version}"
    }
}