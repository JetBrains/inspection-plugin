package org.jetbrains.intellij

import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File
import java.lang.IllegalStateException

class ProxyRunner(jar: File, ideaHomeDirectory: File, private val logger: (LoggerLevel, String) -> Unit) {
    private val connection: Connection.Master

    fun run(parameters: IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>): RunnerOutcome {
        connection.write(Connection.Type.MasterOut.COMMAND, Command.RUN.toString())
        connection.write(Connection.Type.MasterOut.VALUE, parameters.toJson())
        while (true) {
            val (type, data) = connection.read()
            when (type) {
                Connection.Type.SlaveOut.ERROR -> logger(LoggerLevel.ERROR, data)
                Connection.Type.SlaveOut.WARNING -> logger(LoggerLevel.WARNING, data)
                Connection.Type.SlaveOut.INFO -> logger(LoggerLevel.INFO, data)
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
        val javaHome = File(System.getenv("JAVA_HOME"))
        val tools = File(javaHome, "/lib/tools.jar").canonicalFile
        if (!tools.exists()) {
            logger(LoggerLevel.ERROR, "$tools not found, check your JAVA_HOME=$javaHome")
            throw IllegalStateException("$tools not found")
        }
        val ideaClasspath = getIdeaClasspath(ideaHomeDirectory)
        logger(LoggerLevel.INFO, "Idea classpath: $ideaClasspath")
        val classpath = (listOf(jar, tools) + ideaClasspath).joinToString(separator) { it.absolutePath }
        val command = listOf("java", "-cp", classpath, "org.jetbrains.idea.inspections.ProxyRunnerImpl")
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        logger(LoggerLevel.INFO, "Process started: ${command.joinToString(" ")}")
        connection = Connection.Master(process.outputStream, process.inputStream)
    }
}