package org.jetbrains.intellij

import org.json.simple.parser.JSONParser
import org.json.simple.JSONObject
import org.json.simple.JSONArray
import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File


fun IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>.toJson(): String {
    return toJsonObject().toJSONString()
}

fun RunnerOutcome.toJson(): String {
    return JSONObject().apply {
        put("value", this@toJson.toString())
    }.toJSONString()
}

fun String.loadOutcome(): RunnerOutcome {
    val parser = JSONParser()
    val obj = parser.parse(this) as JSONObject
    val value = obj.getFiled<String>("value")
    return RunnerOutcome.valueOf(value)
}

fun String.loadIdeaRunnerParameters(): IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>> {
    val parser = JSONParser()
    val obj = parser.parse(this) as JSONObject
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
        getFiled<Long?>("max")?.toInt()
)

private fun JSONObject.loadReport() = InspectionsRunnerParameters.Report(
        getFiled("isQuiet"),
        getFiled<JSONObject?>("xml")?.loadFile(),
        getFiled<JSONObject?>("html")?.loadFile()
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
        getFiled("ideaVersion"),
        getFiled<JSONObject>("ideaHomeDirectory").loadFile(),
        getFiled<JSONObject>("ideaSystemDirectory").loadFile(),
        getFiled<JSONObject>("kotlinPluginDirectory").loadFile(),
        getFiled<JSONObject>("childParameters").loadFileInfoRunnerParameters()
)

private fun InspectionsRunnerParameters.toJsonObject(): JSONObject = JSONObject().apply {
    put("ideaVersion", ideaVersion)
    put("kotlinPluginVersion", kotlinPluginVersion)
    put("isAvailableCodeChanging", isAvailableCodeChanging)
    put("reportParameters", reportParameters.toJsonObject())
    put("inheritFromIdea", inheritFromIdea)
    put("profileName", profileName)
    put("errors", errors.toJsonObject())
    put("warnings", warnings.toJsonObject())
    put("info", info.toJsonObject())
}

private fun File.toJsonObject(): JSONObject = JSONObject().apply {
    put("path", absolutePath)
}

private fun InspectionsRunnerParameters.Report.toJsonObject(): JSONObject = JSONObject().apply {
    put("isQuiet", isQuiet)
    put("html", html?.toJsonObject())
    put("xml", xml?.toJsonObject())
}

private fun InspectionsRunnerParameters.Inspections.toJsonObject(): JSONObject = JSONObject().apply {
    put("max", max)
    put("inspections", inspections.toJsonObject())
}

private fun Map<String, InspectionsRunnerParameters.Inspection>.toJsonObject(): JSONObject =
        map { it.key to it.value.toJsonObject() }.toMap(JSONObject())

private fun InspectionsRunnerParameters.Inspection.toJsonObject(): JSONObject = JSONObject().apply {
    put("name", name)
    put("quickFix", quickFix)
}

private fun IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>.toJsonObject(): JSONObject = JSONObject().apply {
    put("projectDir", projectDir.toJsonObject())
    put("projectName", projectName)
    put("moduleName", moduleName)
    put("ideaVersion", ideaVersion)
    put("ideaHomeDirectory", ideaHomeDirectory.toJsonObject())
    put("ideaSystemDirectory", ideaSystemDirectory.toJsonObject())
    put("kotlinPluginDirectory", kotlinPluginDirectory.toJsonObject())
    put("childParameters", childParameters.toJsonObject())
}

private fun List<File>.toJsonObject(): JSONArray = map { it.toJsonObject() }.toCollection(JSONArray())

private fun FileInfoRunnerParameters<InspectionsRunnerParameters>.toJsonObject(): JSONObject = JSONObject().apply {
    put("files", files.toJsonObject())
    put("childParameters", childParameters.toJsonObject())
}
