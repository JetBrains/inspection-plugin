package org.jetbrains.idea.inspections

import org.jetbrains.idea.inspections.runners.InspectionsRunner
import org.jetbrains.intellij.*


class ProxyRunnerImpl {
    private val connection = Connection.Slave(System.out, System.`in`)
    private val logger = Logger(connection)
    private var runner = InspectionsRunner(logger)

    fun step(): Boolean {
        try {
            val rawCommand = connection.read(Connection.Type.MasterOut.COMMAND)
            val command = Command.valueOf(rawCommand)
            when (command) {
                Command.RUN -> run()
                Command.FINALIZE -> return true
            }
        } catch (exception: Throwable) {
            logger.exception(exception)
            val result = RunnerOutcome.CRASH.toJson()
            connection.write(Connection.Type.SlaveOut.VALUE, result)
            throw exception
        }
        return false
    }

    private fun run() {
        val parameters = connection.read(Connection.Type.MasterOut.VALUE)
        val proxyRunnerParameters = parameters.loadIdeaRunnerParameters()
        val success = runner.run(proxyRunnerParameters)
        val outcome = if (success) RunnerOutcome.SUCCESS else RunnerOutcome.FAIL
        val result = outcome.toJson()
        connection.write(Connection.Type.SlaveOut.VALUE, result)
    }

    fun finalize() {
        runner.finalize()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val runner = ProxyRunnerImpl()
            try {
                while (!runner.step());
            } finally {
                runner.finalize()
            }
        }
    }
}