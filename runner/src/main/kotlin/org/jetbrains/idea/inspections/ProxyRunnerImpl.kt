package org.jetbrains.idea.inspections

import org.jetbrains.idea.inspections.runners.InspectionsRunner
import org.jetbrains.intellij.*


class ProxyRunnerImpl {
    private val connection = Connection.Slave(System.out, System.`in`)
    private val logger = ProxyLogger(connection)
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
        connection.write(Connection.Type.SlaveOut.VALUE, outcome.toJson())
    }

    fun finalize() {
        try {
            runner.finalize()
            connection.write(Connection.Type.SlaveOut.VALUE, RunnerOutcome.SUCCESS.toJson())
        } catch (ex: Throwable) {
            logger.exception(ex)
            connection.write(Connection.Type.SlaveOut.VALUE, RunnerOutcome.CRASH.toJson())
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val runner = ProxyRunnerImpl()
            try {
                while (!runner.step());
                runner.finalize()
                System.exit(0)
            } catch (ex: Throwable) {
                runner.finalize()
                System.exit(1)
            }
        }
    }
}