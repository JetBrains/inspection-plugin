package org.jetbrains.intellij

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import java.io.File
import java.lang.IllegalArgumentException

class InspectionTool {
    data class Module(val name: String, val directory: File, val parent: Module?)

    data class Task(val name: String, val sourceSets: List<File>, val module: Module?)

    class ToolArgumentsParser(parser: ArgParser) {
        val tasks by parser.positionalList("TASKS",
                help = "Task name for execution, example :module:inspectionsMain")

        val runnerJar by parser.storing("--runner",
                help = "Runner jar file") { File(this) }
                .addValidator {
                    if (!value.isFile) {
                        val message = "Invalid runner jar file: $value"
                        throw IllegalArgumentException(message)
                    }
                }

        val ideaHomeDirectory by parser.storing("--idea",
                help = "Idea home directory") { File(this) }
                .addValidator {
                    if (!value.isDirectory) {
                        val message = "Invalid idea home directory: $value"
                        throw IllegalArgumentException(message)
                    }
                }

        val kotlinPluginHomeDirectory by parser.storing("--kotlin",
                help = "Kotlin plugin home directory") { File(this) }
                .default { null }
                .addValidator {
                    if (value?.isDirectory == false) {
                        val message = "Invalid kotlin plugin home directory: $value"
                        throw IllegalArgumentException(message)
                    }
                }

        val projectDirectory by parser.storing("--project",
                help = "Project directory to analyze") { File(this) }
                .default { null }
                .addValidator {
                    if (value?.isDirectory == false) {
                        val message = "Invalid idea home directory: $value"
                        throw IllegalArgumentException(message)
                    }
                }

        val htmlReport by parser.storing("--html",
                help = "Html report directory") { File(this) }
                .default { null }
                .addValidator {
                    if (value?.isDirectory == false) {
                        val message = "Invalid html report directory: $value"
                        throw IllegalArgumentException(message)
                    }
                }

        val xmlReport by parser.storing("--xml",
                help = "Xml report directory") { File(this) }
                .default { null }
                .addValidator {
                    if (value?.isDirectory == false) {
                        val message = "Invalid xml report directory: $value"
                        throw IllegalArgumentException(message)
                    }
                }
    }

//    private fun getIdeaRunnerParameters(sourceSet: File) =
//            IdeaRunnerParameters(
//
//            )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = mainBody {
            ArgParser(args).parseInto(::ToolArgumentsParser).run {
                println("""
                    tasks = $tasks
                    runnerJar = $runnerJar
                    ideaHomeDirectory = $ideaHomeDirectory
                    kotlinPluginHomeDirectory = $kotlinPluginHomeDirectory
                    projectDirectory = $projectDirectory
                    htmlReport = $htmlReport
                    xmlReport = $xmlReport
                """.trimIndent())
            }
        }
    }
}