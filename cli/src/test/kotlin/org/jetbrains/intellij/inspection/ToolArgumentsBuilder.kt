package org.jetbrains.intellij.inspection

import org.jetbrains.intellij.LoggerLevel
import org.jetbrains.intellij.SettingsBuilder
import org.json.simple.JSONObject
import java.io.File

class ToolArgumentsBuilder {
    var tasks: List<String>? = null
    var level: LoggerLevel? = null
    var settings: SettingsBuilder = SettingsBuilder()
}