package org.jetbrains.intellij

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File


class ConfigurationParser {
    private val parser = JSONParser()

    fun parse(file: File): Configuration {
        val configuration = parser.parse(file.readText()) as JSONObject
        val runner = configuration.getField<String>("runner").loadFile("Runner jar")
        val idea = configuration.getField<String>("idea").loadDir("Idea home")
        val modules = configuration.getField<JSONArray>("modules").loadModules()
        val projectName = configuration.getField<String>("projectName")
        if (modules.find { it.name == projectName } == null) {
            throw IllegalArgumentException("Project module not found")
        }
        val report = configuration.getField<JSONObject?>("report")?.loadReport() ?: Report(false, null, null)
        return Configuration(runner, idea, projectName, modules, report)
    }

    private inline fun <reified T> JSONObject.getField(field: String): T = get(field) as T

    private fun String.loadFile(name: String, checkExistence: Boolean = true) = File(this).apply {
        if (checkExistence && !exists()) throw IllegalArgumentException("$name not found: ${this.absolutePath}")
        if (exists() && !isFile) throw IllegalArgumentException("$name field must be a file")
    }

    private fun String.loadDir(name: String, checkExistence: Boolean = true) = File(this).apply {
        if (checkExistence && !exists()) throw IllegalArgumentException("$name not found: ${this.absolutePath}")
        if (exists() && !isDirectory) throw IllegalArgumentException("$name field must be a directory")
    }

    private fun JSONObject.loadReport() = Report(
            getField<Boolean?>("quiet") ?: false,
            getField<String?>("html")?.loadFile("Html report", checkExistence = false),
            getField<String?>("xml")?.loadFile("Xml report", checkExistence = false)
    )

    private fun JSONArray.loadSourceSets() = map { (it as String).loadDir("Source set") }.toSet()

    private fun JSONObject.loadModule() = Module(
            getField("name"),
            getField<String>("directory").loadDir("Module directory"),
            getField<JSONArray?>("sourceSets")?.loadSourceSets() ?: emptySet()
    )

    private fun JSONArray.loadModules() = map { (it as JSONObject).loadModule() }.toList()
}