package org.jetbrains.intellij

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File

abstract class JsonParser<T> {

    abstract fun Any?.load(): T

    protected inline fun <reified T> JSONObject.getField(field: String): T = get(field) as T

    protected fun String.loadFile(name: String, checkExistence: Boolean = true) = File(this).apply {
        if (checkExistence && !exists()) throw IllegalArgumentException("$name not found: ${this.absolutePath}")
        if (exists() && !isFile) throw IllegalArgumentException("$name field must be a file")
    }

    protected fun String.loadDir(name: String, checkExistence: Boolean = true) = File(this).apply {
        if (checkExistence && !exists()) throw IllegalArgumentException("$name not found: ${this.absolutePath}")
        if (exists() && !isDirectory) throw IllegalArgumentException("$name field must be a directory")
    }

    private val parser = JSONParser()

    fun parse(file: File) = parser.parse(file.readText()).load()
}