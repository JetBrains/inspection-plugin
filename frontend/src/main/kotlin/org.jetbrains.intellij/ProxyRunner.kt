package org.jetbrains.intellij

import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File

class ProxyRunner(jar: File, ideaClasspath: List<File>, private val logger: (Logger.Level, String) -> Unit) {
    private val connection: Connection.Master

    fun run(parameters: IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>): RunnerOutcome {
        connection.write(Connection.Type.MasterOut.COMMAND, Command.RUN.toString())
        connection.write(Connection.Type.MasterOut.VALUE, parameters.toJson())
        while (true) {
            val (type, data) = connection.read()
            when (type) {
                Connection.Type.SlaveOut.ERROR -> logger(Logger.Level.ERROR, data)
                Connection.Type.SlaveOut.WARNING -> logger(Logger.Level.WARNING, data)
                Connection.Type.SlaveOut.INFO -> logger(Logger.Level.INFO, data)
                Connection.Type.SlaveOut.VALUE -> return data.loadOutcome()
                null -> println(data)
            }
        }
    }

    fun finalize() {
        connection.write(Connection.Type.MasterOut.COMMAND, Command.FINALIZE.toString())
    }

    init {
        val separator = System.getProperty("path.separator")
        val tools = File(System.getProperty("java.home"), "../lib/tools.jar").canonicalFile
        val classpath = (listOf(jar, tools) + ideaClasspath).joinToString(separator) { it.absolutePath }
        val command = listOf("java", "-cp", classpath, "org.jetbrains.idea.inspections.ProxyRunnerImpl")
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        logger(Logger.Level.INFO, "Process started: ${command.joinToString(" ")}")
        connection = Connection.Master(process.outputStream, process.inputStream)
    }
}