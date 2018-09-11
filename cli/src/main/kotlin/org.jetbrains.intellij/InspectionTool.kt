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
        val runner = ProxyRunner(arguments.configuration.runner, arguments.configuration.idea) { level, message ->
            when (level) {
                Logger.Level.INFO -> println("InspectionTool: $message")
                Logger.Level.WARNING -> println("InspectionTool: $message")
                Logger.Level.ERROR -> System.err.println("InspectionTool: $message")
            }
        }
        try {
            arguments.toParameters().forEach {
                val outcome = runner.run(it)
                when (outcome) {
                    RunnerOutcome.CRASH -> throw RuntimeException("Execution crashed")
                    RunnerOutcome.FAIL -> throw RuntimeException("Execution failed")
                    RunnerOutcome.SUCCESS -> println("InspectionPlugin: RUN SUCCESS")
                }
            }
        } finally {
            runner.finalize()
        }
    }

    private fun Arguments.toParameters() = tasks.map {
        when (it.name) {
            Task.Name.CHECK -> configuration.checkInspectionParameters(it.module)
            Task.Name.INSPECTIONS -> configuration.inspectionParameters(it.module)
            Task.Name.REFORMAT -> configuration.reformatParameters(it.module)
        }
    }

    private val Configuration.ideaVersion: String
        get() = File(idea, "build.txt").readLines().first()

    private val Configuration.projectDir: File
        get() = modules.first { it.name == projectName }.directory

    private val Module.sources: List<File>
        get() = sourceSets.asSequence().map { it.walk().asSequence() }.flatten().filter { it.isFile }.toList()

    private fun Configuration.checkInspectionParameters(module: Module) = IdeaRunnerParameters(
            projectDir = projectDir,
            projectName = projectName,
            moduleName = module.name,
            ideaVersion = ideaVersion,
            ideaHomeDirectory = idea,
            ideaSystemDirectory = IDEA_SYSTEM_DIRECTORY,
            kotlinPluginDirectory = kotlin,
            childParameters = FileInfoRunnerParameters(
                    files = module.sources,
                    childParameters = InspectionsRunnerParameters(
                            ideaVersion = ideaVersion,
                            kotlinPluginVersion = null,
                            isAvailableCodeChanging = false,
                            reportParameters = report.toParameters(),
                            inheritFromIdea = true,
                            profileName = null,
                            errors = InspectionsRunnerParameters.Inspections(emptyMap(), null),
                            warnings = InspectionsRunnerParameters.Inspections(emptyMap(), null),
                            info = InspectionsRunnerParameters.Inspections(emptyMap(), null)
                    )
            )
    )

    private fun Configuration.inspectionParameters(module: Module) = IdeaRunnerParameters(
            projectDir = projectDir,
            projectName = projectName,
            moduleName = module.name,
            ideaVersion = ideaVersion,
            ideaHomeDirectory = idea,
            ideaSystemDirectory = IDEA_SYSTEM_DIRECTORY,
            kotlinPluginDirectory = kotlin,
            childParameters = FileInfoRunnerParameters(
                    files = module.sources,
                    childParameters = InspectionsRunnerParameters(
                            ideaVersion = ideaVersion,
                            kotlinPluginVersion = null,
                            isAvailableCodeChanging = true,
                            reportParameters = report.toParameters(),
                            inheritFromIdea = true,
                            profileName = null,
                            errors = InspectionsRunnerParameters.Inspections(emptyMap(), null),
                            warnings = InspectionsRunnerParameters.Inspections(emptyMap(), null),
                            info = InspectionsRunnerParameters.Inspections(emptyMap(), null)
                    )
            )
    )

    private fun Configuration.reformatParameters(module: Module) = IdeaRunnerParameters(
            projectDir = projectDir,
            projectName = projectName,
            moduleName = module.name,
            ideaVersion = ideaVersion,
            ideaHomeDirectory = idea,
            ideaSystemDirectory = IDEA_SYSTEM_DIRECTORY,
            kotlinPluginDirectory = kotlin,
            childParameters = FileInfoRunnerParameters(
                    files = module.sources,
                    childParameters = InspectionsRunnerParameters(
                            ideaVersion = ideaVersion,
                            kotlinPluginVersion = null,
                            isAvailableCodeChanging = true,
                            reportParameters = report.toParameters(),
                            inheritFromIdea = false,
                            profileName = null,
                            errors = InspectionsRunnerParameters.Inspections(emptyMap(), null),
                            warnings = InspectionsRunnerParameters.Inspections(
                                    inspections = mapOf(
                                            Pair(
                                                    REFORMAT_INSPECTION_TOOL_PARAMETERS.name,
                                                    REFORMAT_INSPECTION_TOOL_PARAMETERS
                                            )
                                    ),
                                    max = null
                            ),
                            info = InspectionsRunnerParameters.Inspections(emptyMap(), null)
                    )
            )
    )

    private val REFORMAT_INSPECTION_TOOL_PARAMETERS = InspectionsRunnerParameters.Inspection(REFORMAT_INSPECTION_TOOL, true)

    private fun Report.toParameters() = InspectionsRunnerParameters.Report(isQuiet, xml, html)

    private fun parseTask(task: String, modules: List<Module>): List<Task> {
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
        val module = fun Module.() = Module(this.name, directory, sourceSets.filter { sourceSetName == null || it.name == sourceSetName })
        val tasks = targetModules.asSequence().map(module).map { Task(name, it) }.toList()
        val sourceSets = tasks.map { it.module.sourceSets }.flatten()
        if (sourceSets.isEmpty()) when (sourceSetName) {
            null -> throw IllegalArgumentException("Source sets not found")
            else -> throw IllegalArgumentException("Source set not found: $sourceSetName")
        }
        return tasks
    }

    private fun ToolArguments.loadConfiguration(): Configuration {
        val config = config ?: throw IllegalArgumentException("Configuration file must be defined")
        return ConfigurationParser().parse(config)
    }

    private fun ToolArguments.generateConfiguration(): Configuration {
        if (runner == null) throw IllegalArgumentException("Runner jar must be defined")
        if (idea == null) throw IllegalArgumentException("Idea home directory must be defined")
        if (kotlin == null) throw IllegalArgumentException("Kotlin plugin home directory must be defined")
        return ConfigurationGenerator().generate(runner!!, idea!!, kotlin!!, project, html, xml)
    }

    private fun ToolArguments.toArguments(): Arguments {
        val configuration = config?.let { loadConfiguration() } ?: generateConfiguration()
        val tasks = tasks.map { parseTask(it, configuration.modules) }.flatten()
        return Arguments(tasks, level, configuration)
    }

    data class Task(val name: Name, val module: Module) {
        enum class Name(val instance: String) {
            CHECK("checkInspections"),
            INSPECTIONS("inspections"),
            REFORMAT("reformat")
        }
    }

    data class Arguments(val tasks: List<Task>, val level: Logger.Level, val configuration: Configuration)

}