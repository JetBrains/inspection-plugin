package org.jetbrains.intellij

import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

class ProxyRunner(jar: File, ideaHomeDirectory: File, private val logger: Logger) {
    private val connection: Connection.Master
    private val process: Process

    constructor(jar: File, ideaHomeDirectory: File, logger: (LoggerLevel, String) -> Unit) : this(jar, ideaHomeDirectory, Logger(null, logger))

    private fun waitOutcome(): RunnerOutcome {
        while (true) {
            val (type, data) = connection.read()
            when (type) {
                Connection.Type.SlaveOut.ERROR -> logger.error(data)
                Connection.Type.SlaveOut.WARNING -> logger.warn(data)
                Connection.Type.SlaveOut.INFO -> logger.info(data)
                Connection.Type.SlaveOut.VALUE -> return data.loadOutcome()
                null -> println(data)
            }
        }
    }

    fun run(parameters: IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>): RunnerOutcome {
        connection.write(Connection.Type.MasterOut.COMMAND, Command.RUN.toString())
        connection.write(Connection.Type.MasterOut.VALUE, parameters.toJson())
        return waitOutcome()
    }

    fun finalize(): RunnerOutcome {
        connection.write(Connection.Type.MasterOut.COMMAND, Command.FINALIZE.toString())
        val outcome = waitOutcome()
        val hasFinish = process.waitFor(TIMEOUT, TimeUnit.MILLISECONDS)
        if (!hasFinish) {
            logger.error("Runner")
            process.destroy()
        }
        return outcome
    }

    companion object {
        private val File.classpath: List<File>
            get() = listFiles { _, name -> name.endsWith("jar") && "xmlrpc" !in name }?.toList()
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
            logger.error("$tools not found, check your JAVA_HOME=$javaHome")
            throw IllegalStateException("$tools not found")
        }
        val ideaClasspath = getIdeaClasspath(ideaHomeDirectory)
        logger.info("Idea classpath: $ideaClasspath")
        val classpath = (listOf(jar, tools) + ideaClasspath).joinToString(separator) { it.absolutePath }
        val command = listOf("java", "-cp", classpath, "org.jetbrains.idea.inspections.ProxyRunnerImpl")
        process = ProcessBuilder(command).redirectErrorStream(true).start()
        logger.info("Process started: ${command.joinToString(" ")}")
        connection = Connection.Master(process.outputStream, process.inputStream)
    }
}