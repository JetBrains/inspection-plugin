package org.jetbrains.intellij

import com.intellij.codeInspection.InspectionProfileEntry
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import org.junit.Assert.*
import org.gradle.testkit.runner.TaskOutcome.*
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateInspection
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName


class InspectionTest {
    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private lateinit var buildFile: File

    private lateinit var inspectionsFile: File

    private lateinit var sourceKotlinFile: File

    private lateinit var sourceJavaFile: File

    @Before
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
        testProjectDir.newFolder("config", "inspections")
        inspectionsFile = testProjectDir.newFile("config/inspections/inspections.xml")
        testProjectDir.newFolder("src", "main", "kotlin")
        testProjectDir.newFolder("src", "main", "java")
        testProjectDir.newFolder("build")
        sourceKotlinFile = testProjectDir.newFile("src/main/kotlin/main.kt")
        sourceJavaFile = testProjectDir.newFile("src/main/java/Main.java")
    }

    @Test
    fun testHelloWorldTask() {
        val buildFileContent = "task helloWorld {" +
                               "    doLast {" +
                               "        println 'Hello world!'" +
                               "    }" +
                               "}"
        writeFile(buildFile, buildFileContent)

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("helloWorld")
                .build()

        assertTrue(result.output.contains("Hello world!"))
        assertEquals(result.task(":helloWorld").outcome, SUCCESS)
    }

    private fun generateBuildFile(
            kotlinNeeded: Boolean,
            maxErrors: Int = -1,
            maxWarnings: Int = -1,
            showViolations: Boolean = true,
            xmlReport: Boolean = false,
            kotlinVersion: String = "1.1.4"
    ): String {
                return StringBuilder().apply {
            val kotlinGradleDependency = if (kotlinNeeded) """
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    }
                """ else ""
            appendln("""
buildscript {
    repositories {
        mavenCentral()
    }
    $kotlinGradleDependency
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
                    appendln("    maxErrors = $maxWarnings")
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
        }.toString()
    }

    private fun assertInspectionBuild(
            expectedOutcome: TaskOutcome,
            vararg expectedDiagnostics: String,
            expectedInOutput: Boolean = true
    ) {
        val result = try {
            GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments("--info", "--stacktrace", "inspectionsMain")
                    .withPluginClasspath()
                    .build()
        }
        catch (failure: UnexpectedBuildFailure) {
            failure.buildResult
        }

        println(result.output)
        for (diagnostic in expectedDiagnostics) {
            if (expectedInOutput) {
                assertTrue(diagnostic in result.output)
            }
            else {
                assertFalse(diagnostic in result.output)
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
            inspections: List<KClass<out InspectionProfileEntry>>
    ): String {
        return StringBuilder().apply {
            appendln("    <${tagName}s>")
            for (inspectionClass in inspections) {
                appendln("        <$tagName class = \"${inspectionClass.jvmName}\"/>")
            }
            appendln("    </${tagName}s>")
        }.toString()
    }

    private fun generateInspectionFile(
            errors: List<KClass<out InspectionProfileEntry>> = emptyList(),
            warnings: List<KClass<out InspectionProfileEntry>> = emptyList(),
            infos: List<KClass<out InspectionProfileEntry>> = emptyList()
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

    @Test
    fun testInspectionConfigurationJava() {
        val buildFileContent = generateBuildFile(kotlinNeeded = false)
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent = generateInspectionFile(
                warnings = listOf(ClassHasNoToStringMethodInspection::class)
        )
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceJavaFile, "public class Main { private int x = 42; }")

        assertInspectionBuild(
                SUCCESS,
                "Main.java:1:14: Class 'Main' does not override 'toString()' method"
        )
    }

    @Test
    fun testInspectionConfigurationKotlin() {
        val buildFileContent = generateBuildFile(kotlinNeeded = true)
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent = generateInspectionFile(
                warnings = listOf(RedundantVisibilityModifierInspection::class)
        )
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceKotlinFile,
                """
public val x = 42

public val y = 13

                """)

        assertInspectionBuild(
                SUCCESS,
                "main.kt:2:1: Redundant visibility modifier",
                "main.kt:4:1: Redundant visibility modifier"
        )
    }

    @Test
    fun testMaxErrors() {
        val buildFileContent = generateBuildFile(kotlinNeeded = true, maxErrors = 2)
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent = generateInspectionFile(
                errors = listOf(CanBeValInspection::class)
        )
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceKotlinFile,
                """
fun foo(a: Int, b: Int, c: Int): Int {
    var x = a
    var y = b
    var z = c
    return x + y + z
}
                """)

        assertInspectionBuild(
                FAILED,
                "main.kt:3:5: Variable is never modified and can be declared immutable using 'val'",
                "main.kt:4:5: Variable is never modified and can be declared immutable using 'val'",
                "main.kt:5:5: Variable is never modified and can be declared immutable using 'val'"
        )
    }

    @Test
    fun testRedundantModality() {
        val buildFileContent = generateBuildFile(kotlinNeeded = true)
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent = generateInspectionFile(
                warnings = listOf(RedundantModalityModifierInspection::class)
        )
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceKotlinFile,
                """
class My {
    final val x = 42
}
                """)

        assertInspectionBuild(
                SUCCESS,
                "main.kt:3:5: Redundant modality modifier"
        )
    }

    @Test
    fun testConvertToStringTemplate() {
        val buildFileContent = generateBuildFile(kotlinNeeded = true)
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent = generateInspectionFile(
                warnings = listOf(ConvertToStringTemplateInspection::class)
        )
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceKotlinFile,
                """
fun foo(arg: Int) = "(" + arg + ")"
                """)

        assertInspectionBuild(
                SUCCESS,
                "main.kt:2:21: Convert concatenation to template"
        )
    }

    @Test
    fun testShowViolations() {
        val buildFileContent = generateBuildFile(kotlinNeeded = true, showViolations = false)
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent = generateInspectionFile(
                warnings = listOf(CanBeParameterInspection::class)
        )
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceKotlinFile,
                """
class My(val x: Int) {
    val y = x
}
                """)

        assertInspectionBuild(
                SUCCESS,
                "main.kt:2:10: Constructor parameter is never used as a property",
                expectedInOutput = false
        )
    }

    @Test
    fun testXMLOutput() {
        val buildFileContent = generateBuildFile(kotlinNeeded = true, xmlReport = true)
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent = generateInspectionFile(
                warnings = listOf(DataClassPrivateConstructorInspection::class)
        )
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceKotlinFile,
                """
data class My private constructor(val x: Double, val y: Int, val z: String)
                """)

        assertInspectionBuild(
                SUCCESS
        )
        val file = File(testProjectDir.root, "build/report.xml")
        val firstLine = file.readLines().first()
        assertTrue("warning class=\"org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection\"" in firstLine)
        assertTrue("main.kt:2:15: Private data class constructor is exposed via the generated 'copy' method" in firstLine)
    }
}