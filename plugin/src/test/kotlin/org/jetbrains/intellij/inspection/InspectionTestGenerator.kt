package org.jetbrains.intellij.inspection

import org.jetbrains.intellij.extensions.InspectionTypeExtension
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.extensions.ReformatExtension
import org.jetbrains.intellij.allSourceFiles
import org.jetbrains.intellij.kotlinCode
import java.io.File
import java.util.*

class InspectionTestGenerator(private val testsDir: File, private val testDataDir: File) {

    private fun inspections(configuration: InspectionsExtension.() -> Unit) =
            InspectionsExtension(null).apply(configuration)

    private fun InspectionsExtension.errors(configuration: InspectionTypeExtension.() -> Unit) =
            errors.apply(configuration)

    private fun InspectionsExtension.warnings(configuration: InspectionTypeExtension.() -> Unit) =
            warnings.apply(configuration)

    private fun InspectionsExtension.infos(configuration: InspectionTypeExtension.() -> Unit) =
            infos.apply(configuration)

    private fun InspectionsExtension.reformat(configuration: ReformatExtension.() -> Unit) =
            reformat.apply(configuration)

    private fun parseInspectionParameters(parameters: (String) -> String?) = inspections {
        Parameter<Boolean>(parameters("inheritFromIdea")) { inheritFromIdea = it }
        Parameter<String>(parameters("profileName")) { profileName = it }
        Parameter<Int>(parameters("maxErrors")) { errors.max = it }
        Parameter<Int>(parameters("maxWarnings")) { warnings.max = it }
        errors {
            Parameter<Set<String>>(parameters("errors.inspections")) { inspections = it }
            Parameter<Int>(parameters("errors.max")) { max = it }
        }
        warnings {
            Parameter<Set<String>>(parameters("warnings.inspections")) { inspections = it }
            Parameter<Int>(parameters("warnings.max")) { max = it }
        }
        infos {
            Parameter<Set<String>>(parameters("infos.inspections")) { inspections = it }
            Parameter<Int>(parameters("infos.max")) { max = it }
        }
        Parameter<File>(parameters("reportsDir")) { reportsDir = it }
        Parameter<Boolean>(parameters("ignoreFailures")) { isIgnoreFailures = it }
        Parameter<String>(parameters("ideaVersion")) { ideaVersion = it }
        Parameter<String>(parameters("kotlinPluginVersion")) { kotlinPluginVersion = it }
        Parameter<String>(parameters("kotlinPluginLocation")) { kotlinPluginLocation = it }
        Parameter<Boolean>(parameters("isQuiet")) { isQuiet = it }
        Parameter<Boolean>(parameters("quiet")) { isQuiet = it }
        Parameter<Boolean>(parameters("quickFix")) { quickFix = it }
        reformat {
            Parameter<Boolean>(parameters("reformat.isQuiet")) { isQuiet = it }
            Parameter<Boolean>(parameters("reformat.quickFix")) { quickFix = it }
        }
    }

    private fun parseParameter(line: String): Pair<String, String> {
        val assign = line.indexOf('=')
        if (assign == -1) throw IllegalArgumentException("'$line' is not parameter")
        val name = line.substring(0, assign)
        val value = line.substring(assign + 1)
        return name.trim() to value.trim()
    }

    fun generate(taskName: String, taskTestDataDirName: String) {
        val taskTestDataDir = File(testDataDir, taskTestDataDirName)
        require(taskTestDataDir.exists() && taskTestDataDir.isDirectory)
        val methods = ArrayList<String>()
        for (test in taskTestDataDir.listFiles().filter { it.isDirectory }) {
            val config = test.listFiles().find { it.name == "config" }
            val ignore = test.listFiles().find { it.name == "ignore" }
            val sources = test.allSourceFiles.toList()
            if (sources.isEmpty()) throw IllegalArgumentException("Test file in $test not found")
            val sourceCommentLines = fun File.() = readLines()
                    .map { it.trim() }
                    .filter { it.startsWith("//") }
            val sourcesCommentLines = sources.map { it.sourceCommentLines() }.flatten()
            val configParameters = config?.readLines()
                    ?.filter { it.isNotEmpty() }
                    ?.map { parseParameter(it) }
                    ?.toMap() ?: emptyMap()
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
                diagnosticParameters[name] ?: sourceParameters(name) ?: configParameters[name]
            }
            val extension = parseInspectionParameters(parameters)
            val name = test.name.capitalize()
            val base = System.getProperty("user.dir")

            val ignoreAnnotation = ignore?.let { "    @Ignore\n" } ?: ""
            @Suppress("UNNECESSARY_SAFE_CALL")
            val method = /*language=kotlin*/"""
                @Test
                fun test$name() {
                    val extension = InspectionsExtension(null)
                    extension.testMode = true
                    ${extension.inheritFromIdea?.kotlinCode?.let { "extension.inheritFromIdea = $it" } ?: ""}
                    ${extension.profileName?.kotlinCode?.let { "extension.profileName = $it" } ?: ""}
                    ${extension.errors.inspections?.kotlinCode?.let { "extension.errors.inspections = $it" } ?: ""}
                    ${extension.errors.max?.kotlinCode?.let { "extension.errors.max = $it" } ?: ""}
                    ${extension.warnings.inspections?.kotlinCode?.let { "extension.warnings.inspections = $it" } ?: ""}
                    ${extension.warnings.max?.kotlinCode?.let { "extension.warnings.max = $it" } ?: ""}
                    ${extension.infos.inspections?.kotlinCode?.let { "extension.infos.inspections = $it" } ?: ""}
                    ${extension.infos.max?.kotlinCode?.let { "extension.infos.max = $it" } ?: ""}
                    ${extension.reportsDir?.kotlinCode(base)?.let { "extension.reportsDir = $it" } ?: ""}
                    ${extension.isIgnoreFailures.let { if (it) "extension.isIgnoreFailures = $it" else "" }}
                    ${extension.ideaVersion?.kotlinCode?.let { "extension.ideaVersion = $it" } ?: ""}
                    ${extension.kotlinPluginVersion?.kotlinCode?.let { "extension.kotlinPluginVersion = $it" } ?: ""}
                    ${extension.kotlinPluginLocation?.kotlinCode?.let { "extension.kotlinPluginLocation = $it" } ?: ""}
                    ${extension.isQuiet?.kotlinCode?.let { "extension.isQuiet = $it" } ?: ""}
                    ${extension.quickFix?.kotlinCode?.let { "extension.quickFix = $it" } ?: ""}
                    ${extension.reformat.isQuiet?.kotlinCode?.let { "extension.reformat.isQuiet = $it" } ?: ""}
                    ${extension.reformat.quickFix?.kotlinCode?.let { "extension.reformat.quickFix = $it" } ?: ""}
                    testBench.doTest(${test.kotlinCode(base)}, extension)
                }
            """.replaceIndent("    ")
            val removeWhiteLines = fun String.() = split('\n').filter { it.trim().isNotEmpty() }.joinToString("\n")
            methods.add((ignoreAnnotation + method).removeWhiteLines())
        }
        val testClassName = taskName.capitalize() + "TestGenerated"
        val resultFile = File(testsDir, "$testClassName.kt")
        val testClass = /*language=kotlin*/"""
                import org.jetbrains.intellij.extensions.InspectionsExtension
                import org.jetbrains.intellij.inspection.InspectionTestBench
                import org.junit.Test
                import org.junit.Ignore
                import org.junit.Rule
                import org.junit.rules.TemporaryFolder
                import java.io.File

                class $testClassName {

                    @Rule
                    @JvmField
                    val testProjectDir = TemporaryFolder()

                    private val testBench = InspectionTestBench(testProjectDir, ${taskName.kotlinCode})
        """.trimIndent()
        resultFile.writeText(testClass + "\n\n" + methods.joinToString("\n\n") + "\n}")
    }
}
