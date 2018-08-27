package org.jetbrains.intellij

import java.io.*
import java.util.*

sealed class Connection<R : Connection.Type, W : Connection.Type>(
        writeStream: OutputStream,
        readStream: InputStream
) {
    private val identificator = "runner65535b: "
    private val readTemplate = "([^:]*): (.*)".toRegex()
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()
    private val writer = PrintWriter(writeStream)
    private val reader = BufferedReader(InputStreamReader(readStream))

    abstract fun getReadType(type: String): R

    fun read(type: R): String {
        val (actualType, data) = read()
        require(actualType == type) { "Expected: $type but was: $actualType" }
        return data
    }

    fun read(): Pair<R?, String> {
        val raw = reader.readLine()
        requireNotNull(raw) { "Read stream is closed" }
        if (!raw.startsWith(identificator)) return null to raw
        val message = raw.removePrefix(identificator)
        val match = readTemplate.matchEntire(message)
        requireNotNull(match) { "Expected: $readTemplate but was: $message" }
        val (type, string) = match!!.destructured
        val data = String(decoder.decode(string))
        return getReadType(type) to data
    }

    fun write(type: W, data: String) {
        val string = encoder.encodeToString(data.toByteArray())
        val message = identificator + type.toString() + ": " + string
        writer.println(message)
        writer.flush()
    }

    class Master(writeStream: OutputStream, readStream: InputStream)
        : Connection<Type.SlaveOut, Type.MasterOut>(writeStream, readStream) {
        override fun getReadType(type: String) = Type.SlaveOut.valueOf(type)
    }

    class Slave(writeStream: OutputStream, readStream: InputStream)
        : Connection<Type.MasterOut, Type.SlaveOut>(writeStream, readStream) {
        override fun getReadType(type: String) = Type.MasterOut.valueOf(type)
    }

    interface Type {
        enum class MasterOut : Type { COMMAND, VALUE }
        enum class SlaveOut : Type { ERROR, WARNING, INFO, VALUE }
    }
}