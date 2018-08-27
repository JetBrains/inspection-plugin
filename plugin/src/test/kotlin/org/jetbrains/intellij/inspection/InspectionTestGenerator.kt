package org.jetbrains.intellij.inspection

import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.extensions.ReformatExtension
import org.jetbrains.intellij.allSourceFiles
import org.jetbrains.intellij.kotlinCode
import java.io.File
import java.util.*

class InspectionTestGenerator(private val testsDir: File, private val testDataDir: File) {

    private fun inspections(configuration: InspectionPluginExtension.() -> Unit) =
            InspectionPluginExtension(null).apply(configuration)

    private fun InspectionPluginExtension.errors(configuration: InspectionsExtension.() -> Unit) =
            errors.apply(configuration)

    private fun InspectionPluginExtension.warnings(configuration: InspectionsExtension.() -> Unit) =
            warnings.apply(configuration)

    private fun InspectionPluginExtension.infos(configuration: InspectionsExtension.() -> Unit) =
            infos.apply(configuration)

    private fun InspectionPluginExtension.reformat(configuration: ReformatExtension.() -> Unit) =
            reformat.apply(configuration)

    private inline fun <T> Iterable<T>.applyEach(crossinline action: T.() -> Unit) = forEach(action)

    private fun parseInspectionParameters(parameters: (String) -> String?) = inspections {
        Parameter<Boolean>(parameters("inheritFromIdea")) { inheritFromIdea = it }
        Parameter<String>(parameters("profileName")) { profileName = it }
        Parameter<Int>(parameters("maxErrors")) { errors.max = it }
        Parameter<Int>(parameters("maxWarnings")) { warnings.max = it }
        Parameter<Boolean>(parameters("testMode")) { testMode = it }
        errors {
            Parameter<Set<String>>(parameters("errors.inspections"))?.forEach { inspection(it) }
            Parameter<Boolean>(parameters("quickFix")) { inspections.values.applyEach { quickFix = it } }
            Parameter<Int>(parameters("errors.max")) { max = it }
        }
        warnings {
            Parameter<Set<String>>(parameters("warnings.inspections"))?.forEach { inspection(it) }
            Parameter<Boolean>(parameters("quickFix")) { inspections.values.applyEach { quickFix = it } }
            Parameter<Int>(parameters("warnings.max")) { max = it }
        }
        infos {
            Parameter<Set<String>>(parameters("infos.inspections"))?.forEach { inspection(it) }
            Parameter<Boolean>(parameters("quickFix")) { inspections.values.applyEach { quickFix = it } }
            Parameter<Int>(parameters("infos.max")) { max = it }
        }
        Parameter<File>(parameters("reportsDir")) { reportsDir = it }
        Parameter<Boolean>(parameters("ignoreFailures")) { isIgnoreFailures = it }
        Parameter<String>(parameters("idea.version")) { idea.version = it }
        Parameter<String>(parameters("plugins.kotlin.version")) { plugins.kotlin.version = it }
        Parameter<String>(parameters("plugins.kotlin.location")) { plugins.kotlin.location = it }
        Parameter<Boolean>(parameters("isQuiet")) { isQuiet = it }
        Parameter<Boolean>(parameters("quiet")) { isQuiet = it }
        reformat {
            Parameter<Boolean>(parameters("reformat.isQuiet")) { isQuiet = it }
            Parameter<Boolean>(parameters("reformat.quickFix")) { quickFix = it }
        }
    }

    private fun inspections(type: String, extension: InspectionsExtension): List<String> {
        val settings = extension.max?.kotlinCode?.let { "extension.$type.max = $it" }
        val inspections = extension.inspections.map { entry ->
            val inspectionName = entry.key
            val inspectionExtension = entry.value
            val inspection = """extension.$type.inspection("$inspectionName")"""
            val quickFix = inspectionExtension.quickFix?.kotlinCode?.let {
                """extension.$type.inspection("$inspectionName").quickFix = $it"""
            }
            listOf(inspection, quickFix)
        }
        return (inspections.flatten() + settings).filterNotNull()
    }

    fun generate(taskName: String, taskTestDataDirName: String) {
        val taskTestDataDir = File(testDataDir, taskTestDataDirName)
        require(taskTestDataDir.exists() && taskTestDataDir.isDirectory)
        val methods = ArrayList<String>()
        for (test in taskTestDataDir.listFiles().filter { it.isDirectory }) {
            val ignore = test.listFiles().find { it.name == "ignore" }
            val sources = test.allSourceFiles.toList()
            if (sources.isEmpty()) throw IllegalArgumentException("Test file in $test not found")
            val sourceCommentLines = fun File.() = readLines()
                    .map { it.trim() }
                    .filter { it.startsWith("//") }
            val sourcesCommentLines = sources.map { it.sourceCommentLines() }.flatten()
            val sourceParameters = { name: String ->
                sourcesCommentLines.singleOrNull { it.startsWith("// $name =") }?.drop("// $name =".length)?.trim()
            }
            val diagnosticSet = { kind: String ->
                sourcesCommentLines.filter { it.startsWith("// $kind:") }.map { it.drop("// $kind:".length).trim() }.toSet()
            }
            val diagnosticParameters = HashMap<String, String>()
            diagnosticSet("error").let { if (it.isNotEmpty()) diagnosticParameters["errors.inspections"] = it.kotlinCode }
            diagnosticSet("warning").let { if (it.isNotEmpty()) diagnosticParameters["warnings.inspections"] = it.kotlinCode }
            diagnosticSet("info").let { if (it.isNotEmpty()) diagnosticParameters["infos.inspections"] = it.kotlinCode }
            val parameters = { name: String ->
                diagnosticParameters[name] ?: sourceParameters(name)
            }
            val extension = parseInspectionParameters(parameters)
            val name = test.name.capitalize().replace('-', '_')
            val base = System.getProperty("user.dir")

            val ignoreAnnotation = ignore?.let { "@Ignore" } ?: ""
            val methodTemplate = /*language=kotlin*/"""
                <ignore>
                @Test
                fun test$name() {
                    val extension = InspectionPluginExtension(null)
                    extension.testMode = true
                    <extension>
                    testBench.doTest(${test.kotlinCode(base)}, extension)
                }
            """.replaceIndent("    ")

            val methodExtension = with(extension) {
                val errors = inspections("errors", errors)
                val warnings = inspections("warnings", warnings)
                val infos = inspections("infos", infos)
                @Suppress("UNNECESSARY_SAFE_CALL")
                val settings = listOfNotNull(
                        inheritFromIdea?.kotlinCode?.let { "extension.inheritFromIdea = $it" },
                        profileName?.kotlinCode?.let { "extension.profileName = $it" },
                        reportsDir?.kotlinCode(base)?.let { "extension.reportsDir = $it" },
                        isIgnoreFailures.let { if (it) "extension.isIgnoreFailures = $it" else "" },
                        idea.version?.kotlinCode?.let { "extension.idea.version = $it" },
                        plugins.kotlin.version?.kotlinCode?.let { "extension.plugins.kotlin.version = $it" },
                        plugins.kotlin.location?.kotlinCode?.let { "extension.plugins.kotlin.location = $it" },
                        isQuiet?.kotlinCode?.let { "extension.isQuiet = $it" },
                        reformat.quickFix?.kotlinCode?.let { "extension.reformat.quickFix = $it" }
                )
                settings + errors + warnings + infos
            }

            val method = methodTemplate
                    .replace("<ignore>", ignoreAnnotation)
                    .replace("<extension>", methodExtension.joinToString("\n        "))
                    .split('\n')
                    .filter { it.trim().isNotEmpty() }
                    .joinToString("\n")
            methods.add(method)
        }
        val testClassName = taskTestDataDirName.capitalize().replace('-', '_').replace("_", "") + "TestGenerated"
        val resultFile = File(testsDir, "$testClassName.kt")
        val testClass = /*language=kotlin*/"""
                import org.jetbrains.intellij.extensions.InspectionPluginExtension
                import org.jetbrains.intellij.inspection.InspectionTestBench
                import org.junit.Test
                import org.junit.Ignore
                import java.io.File

                class $testClassName {
                    private val testBench = InspectionTestBench(${taskName.kotlinCode})
        """.trimIndent()
        resultFile.writeText(testClass + "\n\n" + methods.joinToString("\n\n") + "\n}")
    }
}
