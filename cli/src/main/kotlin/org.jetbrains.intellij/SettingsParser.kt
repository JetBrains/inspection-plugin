package org.jetbrains.intellij

import org.json.simple.JSONObject

class SettingsParser : JsonParser<Settings>() {
    override fun Any?.load() = (this as JSONObject).loadSettings()

    private fun JSONObject.loadSettings() = Settings(
            getField<String?>("runner")?.loadFile("Runner jar"),
            getField<String?>("idea")?.loadDir("Idea home"),
            getField("inheritFromIdea"),
            getField("profileName"),
            getField<JSONObject?>("report")?.loadReport(),
            getField<JSONObject?>("reformat")?.loadInspection(),
            getField<JSONObject?>("errors")?.loadInspections(),
            getField<JSONObject?>("warnings")?.loadInspections(),
            getField<JSONObject?>("info")?.loadInspections()
    )

    private fun JSONObject.loadInspections() = Settings.Inspections(
            asSequence().filter { it.key != "max" }
                    .map { it.key as String to it.value as JSONObject }
                    .map { it.first to it.second.loadInspection() }
                    .toMap(),
            getField("max")
    )

    private fun JSONObject.loadInspection() = Settings.Inspection(
            getField("quickFix")
    )

    private fun JSONObject.loadReport() = Settings.Report(
            getField("quiet"),
            getField<String?>("html")?.loadFile("Html report", checkExistence = false),
            getField<String?>("xml")?.loadFile("Xml report", checkExistence = false)
    )
}