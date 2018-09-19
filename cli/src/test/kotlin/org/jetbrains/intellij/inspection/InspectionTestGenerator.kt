package org.jetbrains.intellij.inspection

import org.jetbrains.intellij.*
import java.io.File
import java.util.*

class InspectionTestGenerator(private val testsDir: File, private val testDataDir: File) {

    private fun ToolArgumentsBuilder.settings(configuration: SettingsBuilder.() -> Unit) = settings.configuration()

    private fun ToolArgumentsBuilder.kotlinCode(base: File): String {
        val settings = settings.kotlinCode(base).appendIndent("                    ").deleteFirstIndent()
        val argumentsBuilder = "ToolArgumentsBuilder()"
        val configuration = """
                .apply {
                    ${tasks?.kotlinCode.kotlinAssignment("tasks")}
                    ${level?.kotlinCode.kotlinAssignment("level")}
                    ${settings.kotlinAssignment("settings")}
                }
            """.trimIndent()
        return argumentsBuilder + configuration.deleteEmptyLines().deleteEmptyConfiguration()
    }

    private fun String?.kotlinAssignment(name: String) = this?.let { "$name = $it" } ?: ""

    private fun String.appendIndent(indent: String) = split('\n').joinToString("\n") { "$indent$it" }

    private fun String.deleteFirstIndent() = split('\n').asSequence()
            .mapIndexed { i, it -> if (i == 0) it.trimStart() else it }
            .joinToString("\n")

    private fun String.deleteEmptyLines() = split('\n').asSequence().filter { it.trim().isNotEmpty() }.joinToString("\n")

    private fun String.isEmptyConfigurations() = replace("\n", "").replace(" ", "") == ".apply{}"

    private fun String.deleteEmptyConfiguration() = if (isEmptyConfigurations()) "" else this

    private fun inspections(name: String, extension: SettingsBuilder.Inspections): String {
        val settings = extension.max?.kotlinCode?.let { "$name.max = $it" }
        val inspections = extension.inspections.map { entry ->
            val inspectionName = entry.key
            val inspectionExtension = entry.value
            val inspection = """$name.inspections["$inspectionName"] = SettingsBuilder.Inspection()"""
            val configurations = """
                .apply {
                    ${inspectionExtension.quickFix?.kotlinCode.kotlinAssignment("quickFix")}
                }
            """.trimIndent()
            inspection + configurations.deleteEmptyLines().deleteEmptyConfiguration()
        }
        return (inspections + settings).asSequence().filterNotNull().joinToString("\n")
    }

    private fun SettingsBuilder.kotlinCode(base: File): String {
        val settingsBuilder = "SettingsBuilder()"
        val configuration = """
        .apply {
            ${runner?.kotlinCode(base).kotlinAssignment("runner")}
            ${idea?.kotlinCode(base).kotlinAssignment("idea")}
            ${ignoreFailures?.kotlinCode.kotlinAssignment("ignoreFailures")}
            ${inheritFromIdea?.kotlinCode.kotlinAssignment("inheritFromIdea")}
            ${profileName?.kotlinCode.kotlinAssignment("profileName")}
            ${report.isQuiet?.kotlinCode.kotlinAssignment("report.isQuiet")}
            ${report.xml?.kotlinCode.kotlinAssignment("report.xml")}
            ${report.html?.kotlinCode.kotlinAssignment("report.html")}
            ${reformat.quickFix?.kotlinCode.kotlinAssignment("reformat.quickFix")}
            <errors>
            <warnings>
            <info>
        }""".trimIndent()
                .replace("<errors>", inspections("errors", errors).appendIndent("    ").deleteFirstIndent())
                .replace("<warnings>", inspections("warnings", warnings).appendIndent("    ").deleteFirstIndent())
                .replace("<info>", inspections("info", info).appendIndent("    ").deleteFirstIndent())
        return settingsBuilder + configuration.deleteEmptyLines().deleteEmptyConfiguration()
    }

    private fun SettingsBuilder.reformat(configuration: SettingsBuilder.Inspection.() -> Unit) = reformat.configuration()

    private fun SettingsBuilder.errors(configuration: SettingsBuilder.Inspections.() -> Unit) = errors.configuration()

    private fun SettingsBuilder.warnings(configuration: SettingsBuilder.Inspections.() -> Unit) = warnings.configuration()

    private fun SettingsBuilder.info(configuration: SettingsBuilder.Inspections.() -> Unit) = info.configuration()

    private fun SettingsBuilder.error(name: String) = errors.inspections.getOrPut(name) {
        SettingsBuilder.Inspection()
    }

    private fun SettingsBuilder.warning(name: String) = warnings.inspections.getOrPut(name) {
        SettingsBuilder.Inspection()
    }

    private fun SettingsBuilder.info(name: String) = info.inspections.getOrPut(name) {
        SettingsBuilder.Inspection()
    }

    private fun arguments(configuration: ToolArgumentsBuilder.() -> Unit) =
            ToolArgumentsBuilder().apply(configuration)

    private inline fun <T> Iterable<T>.applyEach(crossinline action: T.() -> Unit) = forEach(action)

    private fun parseToolArguments(parameters: (String) -> String?) = arguments {
        Parameter<List<String>>(parameters("tasks")) { tasks = it }
        Parameter<LoggerLevel>(parameters("level")) { level = it }
        settings {
            Parameter<File>(parameters("runner")) { runner = it }
            Parameter<File>(parameters("idea")) { idea = it }
            Parameter<Boolean>(parameters("ignoreFailures")) { ignoreFailures = it }
            Parameter<Boolean>(parameters("inheritFromIdea")) { inheritFromIdea = it }
            Parameter<String>(parameters("profileName")) { profileName = it }
            Parameter<Int>(parameters("maxErrors")) { errors.max = it }
            Parameter<Int>(parameters("maxWarnings")) { warnings.max = it }
            Parameter<List<String>>(parameters("errors"))?.forEach { error(it) }
            Parameter<List<String>>(parameters("warnings"))?.forEach { warning(it) }
            Parameter<List<String>>(parameters("info"))?.forEach { info(it) }
            errors {
                Parameter<Boolean>(parameters("quickFix")) { inspections.values.applyEach { quickFix = it } }
                Parameter<Int>(parameters("errors.max")) { max = it }
            }
            warnings {
                Parameter<Boolean>(parameters("quickFix")) { inspections.values.applyEach { quickFix = it } }
                Parameter<Int>(parameters("warnings.max")) { max = it }
            }
            info {
                Parameter<Boolean>(parameters("quickFix")) { inspections.values.applyEach { quickFix = it } }
                Parameter<Int>(parameters("info.max")) { max = it }
            }
            Parameter<Boolean>(parameters("isQuiet")) { report.isQuiet = it }
            Parameter<Boolean>(parameters("quiet")) { report.isQuiet = it }
            reformat {
                Parameter<Boolean>(parameters("reformat.quickFix")) { quickFix = it }
            }
        }
    }

    fun generate(defaultTaskName: String, taskTestDataDirName: String) {
        val taskTestDataDir = File(testDataDir, taskTestDataDirName)
        require(taskTestDataDir.exists() && taskTestDataDir.isDirectory) {
            "Task test data directory ${taskTestDataDir.absolutePath} not exists"
        }
        val methods = ArrayList<String>()
        for (test in taskTestDataDir.listFiles().filter { it.isDirectory }) {
            val ignore = test.listFiles().find { it.name == "ignore" }
            val sources = test.allSourceFiles.toList()
            if (sources.isEmpty()) throw IllegalArgumentException("Test file in $test not found")
            val sourceCommentLines = fun File.() = readLines().asSequence()
                    .map { it.trim() }
                    .filter { it.startsWith("//") }
                    .toList()
            val sourcesCommentLines = sources.map { it.sourceCommentLines() }.flatten()
            val sourceParameters = { name: String ->
                sourcesCommentLines.singleOrNull { it.startsWith("// $name =") }?.drop("// $name =".length)?.trim()
            }
            val argumentList = { kind: String ->
                sourcesCommentLines.asSequence()
                        .filter { it.startsWith("// $kind:") }
                        .map { it.drop("// $kind:".length).trim() }
                        .toList()
            }
            val listParameters = HashMap<String, String>()
            argumentList("error").let { if (it.isNotEmpty()) listParameters["errors"] = it.kotlinCode }
            argumentList("warning").let { if (it.isNotEmpty()) listParameters["warnings"] = it.kotlinCode }
            argumentList("info").let { if (it.isNotEmpty()) listParameters["info"] = it.kotlinCode }
            argumentList("task").let { if (it.isNotEmpty()) listParameters["tasks"] = it.kotlinCode }
            val parameters = { name: String -> listParameters[name] ?: sourceParameters(name) }
            val arguments = parseToolArguments(parameters)
            val name = test.name.capitalize().replace('-', '_').replace('.', '_').replace(':', '_')
            val base = File(System.getProperty("user.dir"))

            val ignoreAnnotation = ignore?.let { "@Ignore" } ?: ""
            val methodTemplate = /*language=kotlin*/"""
                <ignore>
                @Test
                fun test$name() {
                    val toolArguments = <tool-arguments>
                    testBench.doTest(${test.kotlinCode(base)}, toolArguments)
                }
            """.replaceIndent("    ")
            val method = methodTemplate
                    .replace("<ignore>", ignoreAnnotation)
                    .replace("<tool-arguments>", arguments.kotlinCode(base).appendIndent("        ").deleteFirstIndent())
                    .deleteEmptyLines()
            methods.add(method)
        }
        val testClassName = taskTestDataDirName.capitalize().replace('-', '_').replace("_", "") + "CliTestGenerated"
        val resultFile = File(testsDir, "$testClassName.kt")
        val testClass = /*language=kotlin*/"""
                import org.jetbrains.intellij.SettingsBuilder
                import org.jetbrains.intellij.inspection.ToolArgumentsBuilder
                import org.jetbrains.intellij.inspection.InspectionTestBench
                import org.junit.Test
                import org.junit.Ignore
                import java.io.File

                class $testClassName {
                    private val testBench = InspectionTestBench(${defaultTaskName.kotlinCode})
        """.trimIndent()
        resultFile.writeText(testClass + "\n\n" + methods.joinToString("\n\n") + "\n}")
    }
}
