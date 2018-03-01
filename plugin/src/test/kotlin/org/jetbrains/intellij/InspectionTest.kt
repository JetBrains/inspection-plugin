package org.jetbrains.intellij

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.*
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jetbrains.intellij.InspectionTest.DiagnosticsStatus.SHOULD_BE_ABSENT
import org.jetbrains.intellij.InspectionTest.DiagnosticsStatus.SHOULD_PRESENT
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InspectionTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private fun generateBuildFile(
            kotlinNeeded: Boolean,
            maxErrors: Int = -1,
            maxWarnings: Int = -1,
            ignoreFailures: Boolean = false,
            quiet: Boolean = false,
            xmlReport: Boolean = false,
            htmlReport: Boolean = false,
            kotlinVersion: String = "1.1.3-2",
            configFileName: String = ""
    ): String {
        val templateLines = File("testData/inspection/build.gradle.template").readLines()
        return StringBuilder().apply {
            for (line in templateLines) {
                val template = Regex("<.*>").find(line)
                if (template == null) {
                    appendln(line)
                    continue
                }
                when (template.value.drop(1).dropLast(1)) {
                    "kotlinGradleDependency" -> if (kotlinNeeded) {
                        appendln("        classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion\"")
                    }
                    "kotlinPlugin" -> if (kotlinNeeded) {
                        appendln("    id 'org.jetbrains.kotlin.jvm' version '$kotlinVersion'")
                    }
                    "maxErrors" -> if (maxErrors > -1) {
                        appendln("    maxErrors = $maxErrors")
                    }
                    "maxWarnings" -> if (maxWarnings > -1) {
                        appendln("    maxWarnings = $maxWarnings")
                    }
                    "ignoreFailures" -> if (ignoreFailures) {
                        appendln("    ignoreFailures = $ignoreFailures")
                    }
                    "quiet" -> if (quiet) {
                        appendln("    quiet = true")
                    }
                    "config" -> if (configFileName.isNotEmpty()) {
                        appendln("    config = resources.text.fromFile(\"$configFileName\")")
                    }
                    "xmlDestination" -> if (xmlReport) {
                        appendln("            destination \"build/report.xml\"")
                    }
                    "htmlDestination" -> if (htmlReport) {
                        appendln("            destination \"build/report.html\"")
                    }
                    "kotlin-stdlib" -> if (kotlinNeeded) {
                        appendln("    compile \"org.jetbrains.kotlin:kotlin-stdlib\"")
                    }
                    "kotlin-runtime" -> if (kotlinNeeded) {
                        appendln("    compile \"org.jetbrains.kotlin:kotlin-runtime\"")
                    }
                }
            }
            println(this)
        }.toString()
    }

    private fun assertInspectionBuild(
            expectedOutcome: TaskOutcome,
            expectedDiagnosticsStatus: DiagnosticsStatus,
            vararg expectedDiagnostics: String
    ) {
        // Delay to solve DaemonDisappearedException problem
        Thread.sleep(5000)
        val result = try {
            GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments("--info", "--stacktrace", "inspectionsMain")
                    // This applies classpath from pluginUnderTestMetadata
                    .withPluginClasspath()
                    .apply {
                        // NB: this is necessary to apply actual plugin
                        withPluginClasspath(pluginClasspath)
                    }.build()
        } catch (failure: UnexpectedBuildFailure) {
            println("Exception caught in test: $failure")
            failure.buildResult
        }

        println(result.output)
        for (diagnostic in expectedDiagnostics) {
            when (expectedDiagnosticsStatus) {
                SHOULD_PRESENT -> assertTrue("$diagnostic is not found (but should)",
                        diagnostic in result.output)
                SHOULD_BE_ABSENT -> assertFalse("$diagnostic is found (but should not)", diagnostic in result.output)
            }
        }
        assertEquals(expectedOutcome, result.task(":inspectionsMain")?.outcome)
    }

    private fun writeFile(destination: File, content: String) {
        destination.bufferedWriter().use {
            it.write(content)
        }
    }

    private fun generateInspectionTags(
            tagName: String,
            inspections: List<String>
    ): String {
        return StringBuilder().apply {
            appendln("    <${tagName}s>")
            for (inspectionClass in inspections) {
                appendln("        <$tagName class = \"$inspectionClass\"/>")
            }
            appendln("    </${tagName}s>")
        }.toString()
    }

    private fun generateInspectionFile(
            inheritFromIdea: Boolean = false,
            errors: List<String> = emptyList(),
            warnings: List<String> = emptyList(),
            infos: List<String> = emptyList()
    ): String {
        return StringBuilder().apply {
            appendln("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")
            appendln("<inspections>")
            if (inheritFromIdea) {
                appendln("<inheritFromIdea/>")
            }
            appendln(generateInspectionTags("error", errors))
            appendln(generateInspectionTags("warning", warnings))
            appendln(generateInspectionTags("info", infos))
            appendln("</inspections>")
        }.toString()
    }

    private fun generateInspectionToolTags(
            level: String,
            inspections: List<String>
    ): String {
        return StringBuilder().apply {
            for (inspectionClass in inspections) {
                val shortClass = inspectionClass.substringAfterLast(".").substringBefore("Inspection")
                appendln("""        <inspection_tool class="$shortClass" enabled="true" level="$level" enabled_by_default="true"/>""")
            }
        }.toString()

    }

    private fun generateInspectionProfileFile(
            errors: List<String> = emptyList(),
            warnings: List<String> = emptyList(),
            infos: List<String> = emptyList()
    ): String {
        return StringBuilder().apply {
            appendln("<component name=\"InspectionProjectProfileManager\">")
            appendln("    <profile version=\"1.0\">\n")
            appendln("        <option name=\"myName\" value=\"Project Default\" /> \n")
            appendln(generateInspectionToolTags("ERROR", errors))
            appendln(generateInspectionToolTags("WARNING", warnings))
            appendln(generateInspectionToolTags("INFO", infos))
            appendln("    </profile>")
            appendln("</component>")
        }.toString()
    }

    private enum class DiagnosticsStatus {
        SHOULD_PRESENT,
        SHOULD_BE_ABSENT
    }

    private inner class InspectionTestConfiguration(
            val testFilePath: String,
            val maxErrors: Int = -1,
            val maxWarnings: Int = -1,
            val ignoreFailures: Boolean = false,
            val quiet: Boolean = false,
            val xmlReport: Boolean = false,
            val htmlReport: Boolean = false,
            val kotlinVersion: String = "1.1.3-2",
            val configFileName: String = "",
            val inheritFromIdea: Boolean = false,
            val errors: List<String> = emptyList(),
            val warnings: List<String> = emptyList(),
            val infos: List<String> = emptyList(),
            val expectedOutcome: TaskOutcome = SUCCESS,
            val expectedDiagnosticsStatus: DiagnosticsStatus = SHOULD_PRESENT,
            vararg val expectedDiagnostics: String
    ) {
        fun doTest() {
            val buildFile = testProjectDir.newFile("build.gradle")
            testProjectDir.newFolder("config", "inspections")
            val inspectionsFile = testProjectDir.newFile(
                    if (configFileName.isNotEmpty()) configFileName else "config/inspections/inspections.xml")
            testProjectDir.newFolder("src", "main", "kotlin")
            testProjectDir.newFolder("src", "main", "java")
            testProjectDir.newFolder("build")

            val isKotlinFile = testFilePath.endsWith("kt")
            val buildFileContent = generateBuildFile(
                    isKotlinFile,
                    maxErrors,
                    maxWarnings,
                    ignoreFailures,
                    quiet,
                    xmlReport,
                    htmlReport,
                    kotlinVersion,
                    configFileName
            )
            writeFile(buildFile, buildFileContent)
            val inspectionsFileContent = generateInspectionFile(inheritFromIdea, errors, warnings, infos)
            writeFile(inspectionsFile, inspectionsFileContent)

            if (inheritFromIdea && (errors + warnings + infos).isNotEmpty()) {
                testProjectDir.newFolder(".idea", "inspectionProfiles")
                val inspectionProfileFile = testProjectDir.newFile(".idea/inspectionProfiles/Project_Default.xml")
                val inspectionProfileContent = generateInspectionProfileFile(errors, warnings, infos)
                writeFile(inspectionProfileFile, inspectionProfileContent)
                val profilesSettingFile = testProjectDir.newFile(".idea/inspectionProfiles/profiles_settings.xml")
                writeFile(profilesSettingFile, """
<component name="InspectionProjectProfileManager">
  <settings>
    <option name="PROJECT_PROFILE" value="Project Default" />
    <option name="USE_PROJECT_PROFILE" value="true" />
    <version value="1.0" />
  </settings>
</component>
                """.trimIndent())
            }

            fun buildSourceFileName(): String {
                val sb = StringBuilder()
                sb.append("src/main/")
                if (isKotlinFile) {
                    sb.append("kotlin")
                }
                else {
                    sb.append("java")
                }
                sb.append("/")
                sb.append(testFilePath.replace("\\", "/").substringAfterLast('/'))
                return sb.toString()
            }
            val testFile = File(testFilePath)
            val sourceFile = testProjectDir.newFile(buildSourceFileName())
            testFile.copyTo(sourceFile, overwrite = true)
            assertInspectionBuild(
                    expectedOutcome,
                    expectedDiagnosticsStatus,
                    *expectedDiagnostics
            )
        }
    }

    private fun doTest(testFilePath: String) {
        val testFile = File(testFilePath)
        val lines = testFile.readLines()
        fun getDiagnosticList(diagnosticKind: String) =
                lines.filter { it.startsWith("// $diagnosticKind:") }.map { it.split(":")[1].trim() }

        val errors = getDiagnosticList("error")
        val warnings = getDiagnosticList("warning")
        val infos = getDiagnosticList("info")

        val testFileName = testFilePath.replace("\\", "/").substringAfterLast('/')
        val expectedDiagnostics = lines.filter { it.startsWith("// :") }.map { testFileName + it.drop(3) }

        fun getParameterValue(parameterName: String, defaultValue: String): String {
            val line = lines.singleOrNull { it.startsWith("// $parameterName =") } ?: return defaultValue
            return line.split("=")[1].trim()
        }

        val maxErrors = getParameterValue("maxErrors", "-1").toInt()
        val maxWarnings = getParameterValue("maxWarnings", "-1").toInt()
        val ignoreFailures = getParameterValue("ignoreFailures", "false").toBoolean()
        val quiet = getParameterValue("quiet", "false").toBoolean()
        val xmlReport = getParameterValue("xmlReport", "false").toBoolean()
        val htmlReport = getParameterValue("htmlReport", "false").toBoolean()
        val kotlinVersion = getParameterValue("kotlinVersion", "1.1.3-2")
        val configFileName = getParameterValue("config", "")
        val inheritFromIdea = getParameterValue("inheritFromIdea", "false").toBoolean()

        val expectedDiagnosticsStatus = if (lines.contains("// SHOULD_BE_ABSENT")) SHOULD_BE_ABSENT else SHOULD_PRESENT
        val expectedOutcome = if (lines.contains("// FAIL")) FAILED else SUCCESS

        InspectionTestConfiguration(
                testFilePath,
                maxErrors,
                maxWarnings,
                ignoreFailures,
                quiet,
                xmlReport,
                htmlReport,
                kotlinVersion,
                configFileName,
                inheritFromIdea,
                errors,
                warnings,
                infos,
                expectedOutcome,
                expectedDiagnosticsStatus,
                *expectedDiagnostics.toTypedArray()
        ).doTest()

        if (xmlReport) {
            fun File.toRootElement() = SAXBuilder().build(this).rootElement

            val actualFile = File(testProjectDir.root, "build/report.xml")
            val expectedFile = File(testFilePath.dropLast(testFileName.length) + "report.xml")
            val actualRoot = actualFile.toRootElement()
            val expectedRoot = expectedFile.toRootElement()
            val xmlOutputter = XMLOutputter(Format.getPrettyFormat())
            val actualRepresentation = xmlOutputter.outputString(actualRoot)
            val expectedRepresentation = xmlOutputter.outputString(expectedRoot)
            assertEquals(expectedRepresentation, actualRepresentation)
        }
        if (htmlReport) {
            val actualFile = File(testProjectDir.root, "build/report.html")
            val expectedFile = File(testFilePath.dropLast(testFileName.length) + "report.html")
            assertEquals(expectedFile.readText().trim().replace("\r\n", "\n"),
                    actualFile.readText().trim().replace("\r\n", "\n"))
        }
    }

    @Test
    fun testConfigurationJava() {
        doTest("testData/inspection/configurationJava/Main.java")
    }

    @Test
    fun testConfigurationKotlin() {
        doTest("testData/inspection/configurationKotlin/main.kt")
    }

    @Test
    fun testConvertToStringTemplate() {
        doTest("testData/inspection/convertToStringTemplate/foo.kt")
    }

    @Test
    fun testCustomConfigInheritIdea() {
        doTest("testData/inspection/customConfigInheritFromIdea/different.kt")
    }

    @Test
    fun testDoNotShowViolations() {
        doTest("testData/inspection/doNotShowViolations/My.kt")
    }

    @Test
    fun testHTMLOutput() {
        doTest("testData/inspection/htmlOutput/My.kt")
    }

    @Test
    fun testMaxErrors() {
        doTest("testData/inspection/maxErrors/foo.kt")
    }

    @Test
    fun testMaxWarningsIgnoreFailures() {
        doTest("testData/inspection/maxWarningsIgnoreFailures/foo.kt")
    }

    @Test
    fun testRedundantModality() {
        doTest("testData/inspection/redundantModality/My.kt")
    }

    @Test
    fun testUnusedSymbolByIdeaProfile() {
        doTest("testData/inspection/unusedSymbolByIdeaProfile/test.kt")
    }

    @Test
    fun testWeakWarningNeverBecomesError() {
        doTest("testData/inspection/weakWarningNeverBecomesError/LeakingThis.kt")
    }

    @Test
    fun testXMLOutput() {
        doTest("testData/inspection/xmlOutput/My.kt")
    }
}