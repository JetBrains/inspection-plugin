package org.jetbrains.intellij

import java.io.PrintWriter
import java.io.StringWriter

open class ProxyLogger(private val connection: Connection.Slave) {
    fun error(message: Any? = "") {
        connection.write(Connection.Type.SlaveOut.ERROR, message.toString())
    }

    fun warn(message: Any? = "") {
        connection.write(Connection.Type.SlaveOut.WARNING, message.toString())
    }

    fun info(message: Any? = "") {
        connection.write(Connection.Type.SlaveOut.INFO, message.toString())
    }

    fun exception(exception: Throwable) {
        val writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))
        connection.write(Connection.Type.SlaveOut.ERROR, writer.toString())
    }
}