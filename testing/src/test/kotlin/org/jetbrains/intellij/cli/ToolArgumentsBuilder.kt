package org.jetbrains.intellij.cli

import org.jetbrains.intellij.LoggerLevel
import org.jetbrains.intellij.SettingsBuilder

class ToolArgumentsBuilder {
    var tasks: List<String>? = null
    var level: LoggerLevel? = null
    var settings: SettingsBuilder = SettingsBuilder()
}