package org.jetbrains.intellij

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File

object InspectionTool {

    private const val REFORMAT_INSPECTION_TOOL = "org.jetbrains.kotlin.idea.inspections.ReformatInspection"

    private val IDEA_SYSTEM_DIRECTORY = File(System.getProperty("user.home"), ".GradleInspectionPluginCaches/system")

    @JvmStatic
    fun main(args: Array<String>) = mainBody {
        val arguments = ArgParser(args).parseInto(::ToolArguments).toArguments()
        val logger = Logger(arguments.level) { level, message ->
            when (level) {
                LoggerLevel.ERROR -> System.err.println("InspectionTool: $message")
                LoggerLevel.WARNING -> System.err.println("InspectionTool: $message")
                LoggerLevel.INFO -> println("InspectionTool: $message")
                LoggerLevel.DEBUG -> println("InspectionTool: $message")
            }
        }
        val runner = ProxyRunner(arguments.runner, arguments.idea, logger)
        val ignoreFailures = arguments.settings.ignoreFailures == true
        try {
            arguments.toParameters().forEach {
                val outcome = runner.run(it)
                val hasIgnoring = ignoreFailures && outcome == RunnerOutcome.FAIL
                val correctedOutcome = if (hasIgnoring) RunnerOutcome.SUCCESS else outcome
                val marker = if (hasIgnoring) "(CORRECTED)" else ""
                logger.info("RUN $correctedOutcome $marker")
                when (outcome) {
                    RunnerOutcome.CRASH -> throw RuntimeException("Execution crashed")
                    RunnerOutcome.FAIL -> if (ignoreFailures) throw RuntimeException("Execution failed")
                    RunnerOutcome.SUCCESS -> Unit
                }
            }
        } finally {
            runner.finalize()
        }
    }

    private fun Arguments.toParameters() = tasks.map {
        when (it.name) {
            Task.Name.CHECK -> toCheckInspectionParameters(settings, structure, it.module)
            Task.Name.INSPECTIONS -> toInspectionParameters(settings, structure, it.module)
            Task.Name.REFORMAT -> toReformatParameters(settings, structure, it.module)
        }
    }

    private val Arguments.ideaVersion: String
        get() = File(idea, "build.txt").readLines().first()

    private val Structure.projectDir: File
        get() = modules.first { it.name == projectName }.directory

    private val Structure.Module.sources: List<File>
        get() = sourceSets.asSequence().map { it.walk().asSequence() }.flatten().filter { it.isFile }.toSet().toList()

    private fun Arguments.toCheckInspectionParameters(settings: SettingsBuilder, structure: Structure, module: Structure.Module) = IdeaRunnerParameters(
            projectDir = structure.projectDir,
            projectName = structure.projectName,
            moduleName = module.name,
            ideaVersion = ideaVersion,
            ideaHomeDirectory = idea,
            ideaSystemDirectory = IDEA_SYSTEM_DIRECTORY,
            plugins = emptyList(),
            childParameters = FileInfoRunnerParameters(
                    files = module.sources.toList(),
                    childParameters = InspectionsRunnerParameters(
                            ideaVersion = ideaVersion,
                            kotlinPluginVersion = null,
                            isAvailableCodeChanging = false,
                            reportParameters = settings.report.toParameters(),
                            inheritFromIdea = settings.inheritFromIdea ?: settings.inspections.isEmpty(),
                            profileName = settings.profileName,
                            errors = settings.errors.toParameters(),
                            warnings = settings.warnings.toParameters(),
                            info = settings.info.toParameters()
                    )
            )
    )

    private fun Arguments.toInspectionParameters(settings: SettingsBuilder, structure: Structure, module: Structure.Module) = IdeaRunnerParameters(
            projectDir = structure.projectDir,
            projectName = structure.projectName,
            moduleName = module.name,
            ideaVersion = ideaVersion,
            ideaHomeDirectory = idea,
            ideaSystemDirectory = IDEA_SYSTEM_DIRECTORY,
            plugins = emptyList(),
            childParameters = FileInfoRunnerParameters(
                    files = module.sources,
                    childParameters = InspectionsRunnerParameters(
                            ideaVersion = ideaVersion,
                            kotlinPluginVersion = null,
                            isAvailableCodeChanging = true,
                            reportParameters = settings.report.toParameters(),
                            inheritFromIdea = settings.inheritFromIdea ?: settings.inspections.isEmpty(),
                            profileName = settings.profileName,
                            errors = settings.errors.toParameters(),
                            warnings = settings.warnings.toParameters(),
                            info = settings.info.toParameters()
                    )
            )
    )

    private fun Arguments.toReformatParameters(settings: SettingsBuilder, structure: Structure, module: Structure.Module) = IdeaRunnerParameters(
            projectDir = structure.projectDir,
            projectName = structure.projectName,
            moduleName = module.name,
            ideaVersion = ideaVersion,
            ideaHomeDirectory = idea,
            ideaSystemDirectory = IDEA_SYSTEM_DIRECTORY,
            plugins = emptyList(),
            childParameters = FileInfoRunnerParameters(
                    files = module.sources,
                    childParameters = InspectionsRunnerParameters(
                            ideaVersion = ideaVersion,
                            kotlinPluginVersion = null,
                            isAvailableCodeChanging = true,
                            reportParameters = settings.report.toParameters(),
                            inheritFromIdea = false,
                            profileName = settings.profileName,
                            errors = InspectionsRunnerParameters.Inspections(emptyMap(), null),
                            warnings = InspectionsRunnerParameters.Inspections(
                                    inspections = mapOf(
                                            Pair(
                                                    REFORMAT_INSPECTION_TOOL,
                                                    settings.reformat.toParameters(REFORMAT_INSPECTION_TOOL, true)
                                            )
                                    ),
                                    max = null
                            ),
                            info = InspectionsRunnerParameters.Inspections(emptyMap(), null)
                    )
            )
    )

    private fun SettingsBuilder.Report?.toParameters() = InspectionsRunnerParameters.Report(
            this?.isQuiet ?: false,
            this?.xml,
            this?.html
    )

    private fun SettingsBuilder.Inspections?.toParameters() = InspectionsRunnerParameters.Inspections(
            this?.inspections?.map { it.key to it.value.toParameters(it.key) }?.toMap() ?: emptyMap(),
            this?.max
    )

    private fun SettingsBuilder.Inspection?.toParameters(name: String, defaultQuickFix: Boolean = false) = InspectionsRunnerParameters.Inspection(
            name,
            this?.quickFix ?: defaultQuickFix
    )

    private fun parseTask(task: String, modules: List<Structure.Module>): List<Task> {
        val names = task.split(':').toList()
        val (moduleName, taskName) = when {
            names.size == 1 -> null to names.first()
            names.size == 2 -> names[0] to names[1]
            else -> throw IllegalArgumentException("Wrong task syntax: $task")
        }
        val targetModules = if (moduleName == null) modules else modules.filter { it.name == moduleName }
        val name = Task.Name.values().find { taskName.startsWith(it.instance) }
                ?: throw IllegalArgumentException("Undefined task name: $taskName")
        val sourceSetName = taskName.removePrefix(name.instance).decapitalize().let { if (it.isEmpty()) null else it }
        val module = fun Structure.Module.() = Structure.Module(
                name = this.name,
                directory = directory,
                sourceSets = sourceSets.filterTo(HashSet()) { sourceSetName == null || it.name == sourceSetName }
        )
        val tasks = targetModules.asSequence().map(module).map { Task(name, it) }.toList()
        val sourceSets = tasks.map { it.module.sourceSets }.flatten()
        if (sourceSets.isEmpty()) when (sourceSetName) {
            null -> throw IllegalArgumentException("Source sets not found")
            else -> throw IllegalArgumentException("Source set not found: $sourceSetName")
        }
        return tasks
    }

    private fun ToolArguments.generateStructure(): Structure {
        return StructureGenerator().generate(project)
    }

    private fun ToolArguments.toArguments(): Arguments {
        val structureFile = structure ?: File("inspections-structure.json").let { if (it.isFile) it else null }
        val settingsFile = settings ?: File("inspections-settings.json").let { if (it.isFile) it else null }
        val structure = structureFile?.let { StructureParser().parse(it) } ?: generateStructure()
        val settings = settingsFile?.let { SettingsParser().parse(it) } ?: SettingsBuilder()
        val tasks = tasks.map { parseTask(it, structure.modules) }.flatten()
        val runner = runner ?: settings.runner ?: throw IllegalArgumentException("Runner jar must be defined")
        val idea = idea ?: settings.idea ?: throw IllegalArgumentException("Idea home directory must be defined")
        val ignoreFailures = ignoreFailures || settings.ignoreFailures ?: false
        return Arguments(tasks, ignoreFailures, idea, runner, level, structure, settings)
    }

    data class Task(val name: Name, val module: Structure.Module) {
        enum class Name(val instance: String) {
            CHECK("checkInspections"),
            INSPECTIONS("inspections"),
            REFORMAT("reformat")
        }
    }

    data class Arguments(
            val tasks: List<Task>,
            val ignoreFailures: Boolean,
            val idea: File,
            val runner: File,
            val level: LoggerLevel,
            val structure: Structure,
            val settings: SettingsBuilder
    )
}