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
            "--error" to LoggerLevel.ERROR,
            help = "Writable logging level")
            .default { LoggerLevel.WARNING }

    val config: File? by parser.storing("--config",
            help = "Configuration file") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isFile == false) {
                    throw IllegalArgumentException("Invalid configuration file: $value")
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

    val kotlin by parser.storing("--kotlin",
            help = "Kotlin plugin home directory") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isDirectory == false) {
                    val message = "Invalid kotlin plugin home directory: $value"
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

    val html by parser.storing("--html",
            help = "Html report directory") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isFile == false) {
                    val message = "Invalid html report file: $value"
                    throw IllegalArgumentException(message)
                }
            }

    val xml by parser.storing("--xml",
            help = "Xml report directory") { File(this) }
            .default { null }
            .addValidator {
                if (value?.isFile == false) {
                    val message = "Invalid xml report file: $value"
                    throw IllegalArgumentException(message)
                }
            }
}