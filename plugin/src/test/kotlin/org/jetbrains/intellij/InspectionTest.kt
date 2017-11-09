package org.jetbrains.intellij

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.*
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.intellij.InspectionTest.DiagnosticsStatus.SHOULD_BE_ABSENT
import org.jetbrains.intellij.InspectionTest.DiagnosticsStatus.SHOULD_PRESENT
import org.junit.Rule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport
import org.junit.rules.TemporaryFolder

@EnableRuleMigrationSupport
class InspectionTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private fun generateBuildFile(
            kotlinNeeded: Boolean,
            maxErrors: Int = -1,
            maxWarnings: Int = -1,
            showViolations: Boolean = true,
            xmlReport: Boolean = false,
            kotlinVersion: String = "1.1.3-2"
    ): String {
        return StringBuilder().apply {
            val kotlinGradleDependency = if (kotlinNeeded) """
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
                """ else ""
            appendln("""
buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        $kotlinGradleDependency
    }
}
                """)
            val kotlinPlugin = if (kotlinNeeded) "id 'org.jetbrains.kotlin.jvm' version '$kotlinVersion'" else ""
            appendln("""
plugins {
    id 'java'
    $kotlinPlugin
    id 'org.jetbrains.intellij.inspections'
}
                """)
            if (maxErrors > -1 || maxWarnings > -1 || !showViolations) {
                appendln("inspections {")
                if (maxErrors > -1) {
                    appendln("    maxErrors = $maxErrors")
                }
                if (maxWarnings > -1) {
                    appendln("    maxWarnings = $maxWarnings")
                }
                if (!showViolations) {
                    appendln("    showViolations = false")
                }
                appendln("}")
            }
            if (xmlReport) {
                appendln("""
inspectionsMain {
    reports {
        xml {
            destination "build/report.xml"
        }
    }
}
                    """)
            }

            appendln("""
sourceSets {
    main {
        java {
            srcDirs = ['src']
        }
    }
}
                """)
            if (kotlinNeeded) {
                appendln("""
repositories {
    mavenCentral()
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib"
    compile "org.jetbrains.kotlin:kotlin-runtime"
}
                    """)
            }
            println(this)
        }.toString()
    }

    private fun assertInspectionBuild(
            expectedOutcome: TaskOutcome,
            expectedDiagnosticsStatus: DiagnosticsStatus,
            vararg expectedDiagnostics: String
    ) {
        val result = try {
            GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments("--info", "--stacktrace", "inspectionsMain")
                    .withPluginClasspath()
                    .apply {
                        withPluginClasspath(pluginClasspath)
                    }.build()
        } catch (failure: UnexpectedBuildFailure) {
            println("Exception caught in test: $failure")
            failure.buildResult
        }

        println(result.output)
        for (diagnostic in expectedDiagnostics) {
            when (expectedDiagnosticsStatus) {
                SHOULD_PRESENT -> assertTrue(diagnostic in result.output)
                SHOULD_BE_ABSENT -> assertFalse(diagnostic in result.output)
            }
        }
        assertEquals(expectedOutcome, result.task(":inspectionsMain").outcome)
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
            errors: List<String> = emptyList(),
            warnings: List<String> = emptyList(),
            infos: List<String> = emptyList()
    ): String {
        return StringBuilder().apply {
            appendln("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")
            appendln("<inspections>")
            appendln(generateInspectionTags("error", errors))
            appendln(generateInspectionTags("warning", warnings))
            appendln(generateInspectionTags("info", infos))
            appendln("</inspections>")
        }.toString()
    }

    private enum class DiagnosticsStatus {
        SHOULD_PRESENT,
        SHOULD_BE_ABSENT
    }

    private inner class InspectionTestConfiguration(
            val maxErrors: Int = -1,
            val maxWarnings: Int = -1,
            val showViolations: Boolean = true,
            val xmlReport: Boolean = false,
            val kotlinVersion: String = "1.1.3-2",
            val errors: List<String> = emptyList(),
            val warnings: List<String> = emptyList(),
            val infos: List<String> = emptyList(),
            val javaText: String? = null,
            val kotlinText: String? = null,
            val expectedOutcome: TaskOutcome = SUCCESS,
            vararg val expectedDiagnostics: String,
            val expectedDiagnosticsStatus: DiagnosticsStatus = SHOULD_PRESENT
    ) {
        fun doTest() {
            val buildFile = testProjectDir.newFile("build.gradle")
            testProjectDir.newFolder("config", "inspections")
            val inspectionsFile = testProjectDir.newFile("config/inspections/inspections.xml")
            testProjectDir.newFolder("src", "main", "kotlin")
            testProjectDir.newFolder("src", "main", "java")
            testProjectDir.newFolder("build")
            val sourceJavaFile = testProjectDir.newFile("src/main/java/Main.java")

            val buildFileContent = generateBuildFile(
                    kotlinText != null,
                    maxErrors,
                    maxWarnings,
                    showViolations,
                    xmlReport,
                    kotlinVersion
            )
            writeFile(buildFile, buildFileContent)
            val inspectionsFileContent = generateInspectionFile(errors, warnings, infos)
            writeFile(inspectionsFile, inspectionsFileContent)
            javaText?.let { writeFile(sourceJavaFile, it) }
            if (kotlinText != null) {
                val sourceKotlinFile = testProjectDir.newFile("src/main/kotlin/main.kt")
                writeFile(sourceKotlinFile, kotlinText)
            }
            assertInspectionBuild(
                    expectedOutcome,
                    expectedDiagnosticsStatus,
                    *expectedDiagnostics
            )
        }
    }

    @Test
    fun testHelloWorldTask() {
        val buildFileContent = "task helloWorld {" +
                               "    doLast {" +
                               "        println 'Hello world!'" +
                               "    }" +
                               "}"
        val buildFile = testProjectDir.newFile("build.gradle")
        writeFile(buildFile, buildFileContent)

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("helloWorld")
                .build()

        assertTrue(result.output.contains("Hello world!"))
        assertEquals(result.task(":helloWorld").outcome, SUCCESS)
    }

    @Test
    fun testInspectionConfigurationJava() {
        InspectionTestConfiguration(
                warnings = listOf("org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection"),
                javaText = "public class Main { private int x = 42; }",
                expectedDiagnostics = "Main.java:1:14: Class 'Main' does not override 'toString()' method"
        ).doTest()
    }

    @Test
    fun testInspectionConfigurationKotlin() {
        InspectionTestConfiguration(
                warnings = listOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection"),
                kotlinText =                 """
public val x = 42

public val y = 13

                """,
                expectedDiagnostics = *arrayOf(
                        "main.kt:2:1: Redundant visibility modifier",
                        "main.kt:4:1: Redundant visibility modifier"
                )
        ).doTest()
    }

    @Test
    fun testMaxErrors() {
        InspectionTestConfiguration(
                errors = listOf("org.jetbrains.kotlin.idea.inspections.CanBeValInspection"),
                maxErrors = 2,
                kotlinText =                 """
fun foo(a: Int, b: Int, c: Int): Int {
    var x = a
    var y = b
    var z = c
    return x + y + z
}
                """,
                expectedOutcome = FAILED,
                expectedDiagnostics = *arrayOf(
                        "main.kt:3:5: Variable is never modified and can be declared immutable using 'val'",
                        "main.kt:4:5: Variable is never modified and can be declared immutable using 'val'",
                        "main.kt:5:5: Variable is never modified and can be declared immutable using 'val'"                )
        ).doTest()
    }

    @Test
    fun testRedundantModality() {
        InspectionTestConfiguration(
                warnings = listOf("org.jetbrains.kotlin.idea.inspections.RedundantModalityModifierInspection"),
                kotlinText =                 """
class My {
    final val x = 42
}
                """,
                expectedDiagnostics = *arrayOf(
                        "main.kt:3:5: Redundant modality modifier"
                )
        ).doTest()
    }

    @Test
    fun testConvertToStringTemplate() {
        InspectionTestConfiguration(
                warnings = listOf("org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateInspection"),
                kotlinText =                 """
fun foo(arg: Int) = "(" + arg + ")"
                """,
                expectedDiagnostics = *arrayOf(
                        "main.kt:2:21: Convert concatenation to template"
                )
        ).doTest()
    }

    @Test
    fun testShowViolations() {
        InspectionTestConfiguration(
                warnings = listOf("org.jetbrains.kotlin.idea.inspections.CanBeParameterInspection"),
                showViolations = false,
                kotlinText =                 """
class My(val x: Int) {
    val y = x
}
                """,
                expectedDiagnostics = *arrayOf(
                        "main.kt:2:10: Constructor parameter is never used as a property"
                ),
                expectedDiagnosticsStatus = SHOULD_BE_ABSENT
        ).doTest()
    }

    @Test
    fun testXMLOutput() {
        InspectionTestConfiguration(
                warnings = listOf("org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection"),
                xmlReport = true,
                kotlinText =                 """
data class My private constructor(val x: Double, val y: Int, val z: String)
                """
        ).doTest()
        val file = File(testProjectDir.root, "build/report.xml")
        val lines = file.readLines()
        val allLines = lines.joinToString(separator = " ")
        assertTrue("warning class=\"org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection\"" in allLines)
        assertTrue("main.kt:2:15: Private data class constructor is exposed via the generated 'copy' method" in allLines)
    }
}