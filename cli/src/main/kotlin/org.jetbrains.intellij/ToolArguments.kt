package org.jetbrains.intellij

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File

class ToolArguments(parser: ArgParser) {

    val tasks by parser.positionalList("TASKS",
            help = "Task name for execution, example :module:inspectionsMain")

    val level by parser.mapping(
            "--info" to LoggerLevel.INFO,
            "--warn" to LoggerLevel.WARNING,
            "--warning" to LoggerLevel.WARNING,
            "--error" to LoggerLevel.ERROR,
            help = "Writable logging level")
            .default { LoggerLevel.WARNING }

    val structure: File? by parser.storing("--structure",
            help = "Structure file") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isFile == false) {
                    throw IllegalArgumentException("Invalid structure file: $value")
                }
            }

    val settings: File? by parser.storing("--settings",
            help = "Settings file") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isFile == false) {
                    throw IllegalArgumentException("Invalid settings file: $value")
                }
            }

    val runner by parser.storing("--runner",
            help = "Runner jar file") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isFile == false) {
                    val message = "Invalid runner jar file: $value"
                    throw IllegalArgumentException(message)
                }
            }

    val idea by parser.storing("--idea",
            help = "Idea home directory") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isDirectory == false) {
                    val message = "Invalid idea home directory: $value"
                    throw IllegalArgumentException(message)
                }
            }

    val project by parser.storing("--project",
            help = "Project directory to analyze") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isDirectory == false) {
                    val message = "Invalid idea home directory: $value"
                    throw IllegalArgumentException(message)
                }
            }
}