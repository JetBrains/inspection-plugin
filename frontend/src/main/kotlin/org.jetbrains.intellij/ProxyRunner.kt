package org.jetbrains.intellij

import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File
import java.lang.IllegalStateException

class ProxyRunner(jar: File, ideaHomeDirectory: File, private val logger: (Logger.Level, String) -> Unit) {
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

    companion object {
        private val File.classpath: List<File>
            get() = listFiles { file, name -> name.endsWith("jar") && "xmlrpc" !in name }?.toList()
                    ?: throw IllegalStateException("Files not found in directory $this")

        private fun getIdeaClasspath(ideaDirectory: File): List<File> {
            val ideaLibraries = File(ideaDirectory, "lib")
            return ideaLibraries.classpath
        }
    }

    init {
        val separator = System.getProperty("path.separator")
        val tools = File(System.getProperty("java.home"), "../lib/tools.jar").canonicalFile
        val ideaClasspath = getIdeaClasspath(ideaHomeDirectory)
        logger(Logger.Level.INFO, "Idea classpath: $ideaClasspath")
        val classpath = (listOf(jar, tools) + ideaClasspath).joinToString(separator) { it.absolutePath }
        val command = listOf("java", "-cp", classpath, "org.jetbrains.idea.inspections.ProxyRunnerImpl")
//        val command = listOf("java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005", "-cp", classpath, "org.jetbrains.idea.inspections.ProxyRunnerImpl")
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        logger(Logger.Level.INFO, "Process started: ${command.joinToString(" ")}")
        connection = Connection.Master(process.outputStream, process.inputStream)
    }
}