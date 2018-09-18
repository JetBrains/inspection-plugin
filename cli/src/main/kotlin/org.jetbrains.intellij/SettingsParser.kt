package org.jetbrains.intellij

import org.json.simple.JSONObject

class SettingsParser : JsonParser<SettingsBuilder>() {
    override fun Any?.load() = (this as JSONObject).loadSettings()

    private fun JSONObject.loadSettings() = SettingsBuilder(
            getField<String?>("runner")?.loadFile("Runner jar"),
            getField<String?>("idea")?.loadDir("Idea home"),
            getField("ignoreFailures"),
            getField("inheritFromIdea"),
            getField("profileName"),
            getField<JSONObject?>("report")?.loadReport() ?: SettingsBuilder.Report(),
            getField<JSONObject?>("reformat")?.loadInspection() ?: SettingsBuilder.Inspection(),
            getField<JSONObject?>("errors")?.loadInspections() ?: SettingsBuilder.Inspections(),
            getField<JSONObject?>("warnings")?.loadInspections() ?: SettingsBuilder.Inspections(),
            getField<JSONObject?>("info")?.loadInspections() ?: SettingsBuilder.Inspections()
    )

    private fun JSONObject.loadInspections() = SettingsBuilder.Inspections(
            asSequence().filter { it.key != "max" }
                    .map { it.key as String to it.value as JSONObject }
                    .map { it.first to it.second.loadInspection() }
                    .toMap().toMutableMap(),
            getField<Long?>("max")?.toInt()
    )

    private fun JSONObject.loadInspection() = SettingsBuilder.Inspection(
            getField("quickFix")
    )

    private fun JSONObject.loadReport() = SettingsBuilder.Report(
            getField("quiet"),
            getField<String?>("html")?.loadFile("Html report", checkExistence = false),
            getField<String?>("xml")?.loadFile("Xml report", checkExistence = false)
    )
}