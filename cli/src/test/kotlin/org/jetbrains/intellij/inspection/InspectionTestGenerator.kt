package org.jetbrains.intellij.inspection

import org.jetbrains.intellij.*
import java.io.File
import java.util.*

class InspectionTestGenerator(private val testsDir: File, private val testDataDir: File) {

    class ToolArgumentsBuilder {
        var errors: List<String>? = null
        var warnings: List<String>? = null
        var info: List<String>? = null
        var tasks: List<String>? = null
        var level: LoggerLevel? = null
        var config: File? = null
        var runner: File? = null
        var idea: File? = null
        var project: File? = null

        fun toString(base: File): String {
            return /*language=kotlin*/"""
                ToolArguments(
                    errors = ${errors?.kotlinCode},
                    warnings = ${warnings?.kotlinCode},
                    info = ${info?.kotlinCode},
                    tasks = ${tasks?.kotlinCode},
                    level = ${level?.kotlinCode},
                    config = ${config?.kotlinCode(base)},
                    runner = ${runner?.kotlinCode(base)},
                    idea = ${idea?.kotlinCode(base)},
                    project = ${project?.kotlinCode(base)}
                )
            """.trimIndent()
        }
    }

    private fun arguments(toolArguments: ToolArgumentsBuilder.() -> Unit) =
            ToolArgumentsBuilder().apply(toolArguments)

    private fun parseToolArguments(parameters: (String) -> String?) = arguments {
        Parameter<List<String>>(parameters("errors")) { errors = it }
        Parameter<List<String>>(parameters("warnings")) { warnings = it }
        Parameter<List<String>>(parameters("info")) { info = it }
        Parameter<List<String>>(parameters("tasks")) { tasks = it }
        Parameter<LoggerLevel>(parameters("level")) { level = it }
        Parameter<File>(parameters("config")) { config = it }
        Parameter<File>(parameters("runner")) { runner = it }
        Parameter<File>(parameters("idea")) { idea = it }
        Parameter<File>(parameters("project")) { project = it }
//        Parameter<Boolean>(parameters("htmlReport")) { if (it) html = File("build/report.html") }
//        Parameter<Boolean>(parameters("xmlReport")) { if (it) xml = File("build/report.xml") }
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
            val diagnosticList = { kind: String ->
                sourcesCommentLines.asSequence()
                        .filter { it.startsWith("// $kind:") }
                        .map { it.drop("// $kind:".length).trim() }
                        .toList()
            }
            val diagnosticParameters = HashMap<String, String>()
            diagnosticList("error").let { if (it.isNotEmpty()) diagnosticParameters["errors"] = it.kotlinCode }
            diagnosticList("warning").let { if (it.isNotEmpty()) diagnosticParameters["warnings"] = it.kotlinCode }
            diagnosticList("info").let { if (it.isNotEmpty()) diagnosticParameters["info"] = it.kotlinCode }
            val parameters = { name: String ->
                diagnosticParameters[name] ?: sourceParameters(name)
            }
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
                    .replace("<tool-arguments>", arguments.toString(base).split('\n').joinToString("\n        "))
                    .split('\n')
                    .asSequence()
                    .filter { it.trim().isNotEmpty() }
                    .joinToString("\n")
            methods.add(method)
        }
        val testClassName = taskTestDataDirName.capitalize().replace('-', '_').replace("_", "") + "TestGenerated"
        val resultFile = File(testsDir, "$testClassName.kt")
        val testClass = /*language=kotlin*/"""
                import org.jetbrains.intellij.inspection.ToolArguments
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
