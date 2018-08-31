package org.jetbrains.intellij

import org.json.simple.parser.JSONParser
import org.json.simple.JSONObject
import org.json.simple.JSONArray
import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File


object ParameterManager {
    fun toJson(parameters: IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>): String {
        return parameters.toJson().toJSONString()
    }

    fun fromJson(parameters: String): IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>> {
        val parser = JSONParser()
        val obj = parser.parse(parameters) as JSONObject
        return obj.loadIdeaRunnerParameters()
    }

    private inline fun <reified T> JSONObject.getFiled(field: String): T = get(field) as T

    private fun JSONObject.loadInspection() = InspectionsRunnerParameters.Inspection(
            getFiled("name"),
            getFiled("quickFix")
    )

    private fun JSONObject.loadInspectionsMap() = map {
        it.key.toString() to (it.value as JSONObject).loadInspection()
    }.toMap()

    private fun JSONObject.loadInspections() = InspectionsRunnerParameters.Inspections(
            getFiled<JSONObject>("inspections").loadInspectionsMap(),
            getFiled("max")
    )

    private fun JSONObject.loadReport() = InspectionsRunnerParameters.Report(
            getFiled("isQuiet"),
            getFiled<JSONObject?>("xml")?.loadFile(),
            getFiled<JSONObject?>("xml")?.loadFile()
    )

    private fun JSONObject.loadFile() = File(
            getFiled<String>("path")
    )

    private fun JSONObject.loadInspectionsRunnerParameters() = InspectionsRunnerParameters(
            getFiled("ideaVersion"),
            getFiled("kotlinPluginVersion"),
            getFiled("isAvailableCodeChanging"),
            getFiled<JSONObject>("reportParameters").loadReport(),
            getFiled("inheritFromIdea"),
            getFiled("profileName"),
            getFiled<JSONObject>("errors").loadInspections(),
            getFiled<JSONObject>("warnings").loadInspections(),
            getFiled<JSONObject>("info").loadInspections()
    )

    private fun JSONObject.loadFileInfoRunnerParameters() = FileInfoRunnerParameters(
            getFiled<JSONArray>("files").loadFiles(),
            getFiled<JSONObject>("childParameters").loadInspectionsRunnerParameters()
    )

    private fun JSONArray.loadFiles() = map { it as JSONObject }.map { it.loadFile() }.toList()

    private fun JSONObject.loadIdeaRunnerParameters() = IdeaRunnerParameters(
            getFiled<JSONObject>("projectDir").loadFile(),
            getFiled("projectName"),
            getFiled("moduleName"),
            getFiled<JSONObject>("ideaHomeDirectory").loadFile(),
            getFiled<JSONObject>("ideaSystemDirectory").loadFile(),
            getFiled<JSONArray>("plugins").loadFiles(),
            getFiled<JSONObject>("childParameters").loadFileInfoRunnerParameters()
    )

    private fun InspectionsRunnerParameters.toJson(): JSONObject = JSONObject().apply {
        put("ideaVersion", ideaVersion)
        put("kotlinPluginVersion", kotlinPluginVersion)
        put("isAvailableCodeChanging", isAvailableCodeChanging)
        put("reportParameters", reportParameters.toJson())
        put("inheritFromIdea", inheritFromIdea)
        put("profileName", profileName)
        put("errors", errors.toJson())
        put("warnings", warnings.toJson())
        put("info", info.toJson())
    }

    private fun File.toJson(): JSONObject = JSONObject().apply {
        put("path", absolutePath)
    }

    private fun InspectionsRunnerParameters.Report.toJson(): JSONObject = JSONObject().apply {
        put("isQuiet", isQuiet)
        put("html", html?.toJson())
        put("xml", xml?.toJson())
    }

    private fun InspectionsRunnerParameters.Inspections.toJson(): JSONObject = JSONObject().apply {
        put("max", max)
        put("inspections", inspections.toJson())
    }

    private fun Map<String, InspectionsRunnerParameters.Inspection>.toJson(): JSONObject =
            map { it.key to it.value.toJson() }.toMap(JSONObject())

    private fun InspectionsRunnerParameters.Inspection.toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("quickFix", quickFix)
    }

    private fun IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>.toJson(): JSONObject = JSONObject().apply {
        put("projectDir", projectDir.toJson())
        put("projectName", projectName)
        put("moduleName", moduleName)
        put("ideaHomeDirectory", ideaHomeDirectory.toJson())
        put("ideaSystemDirectory", ideaSystemDirectory.toJson())
        put("plugins", plugins.toJson())
        put("childParameters", childParameters.toJson())
    }

    private fun List<File>.toJson(): JSONArray = map { it.toJson() }.toCollection(JSONArray())

    private fun FileInfoRunnerParameters<InspectionsRunnerParameters>.toJson(): JSONObject = JSONObject().apply {
        put("files", files.toJson())
        put("childParameters", childParameters.toJson())
    }
}
