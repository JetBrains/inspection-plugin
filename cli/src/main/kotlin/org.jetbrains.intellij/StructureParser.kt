package org.jetbrains.intellij

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File


class StructureParser : JsonParser<Structure>() {
    override fun Any?.load() = (this as JSONObject).loadStructure()

    private fun JSONObject.loadStructure(): Structure {
        val modules = getField<JSONArray>("modules").loadModules()
        val projectName = getField<String>("projectName")
        if (modules.find { it.name == projectName } == null) {
            throw IllegalArgumentException("Project module not found")
        }
        return Structure(projectName, modules)
    }

    private fun JSONArray.loadSourceSets() = map { (it as String).loadDir("Source set") }.toSet()

    private fun JSONObject.loadModule() = Structure.Module(
            getField("name"),
            getField<String>("directory").loadDir("Module directory"),
            getField<JSONArray?>("sourceSets")?.loadSourceSets() ?: emptySet()
    )

    private fun JSONArray.loadModules() = map { (it as JSONObject).loadModule() }.toList()
}