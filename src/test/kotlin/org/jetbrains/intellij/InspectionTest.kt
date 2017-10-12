package org.jetbrains.intellij

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import org.junit.Assert.*
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection
import org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateInspection
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

    @Test
    fun testInspectionConfigurationJava() {
        val buildFileContent =
                """
buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id 'java'
    id 'org.jetbrains.intellij.inspections'
}

sourceSets {
    main {
        java {
            srcDirs = ['src']
        }
    }
}
                """
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent =
                """<?xml version="1.0" encoding="UTF-8" ?>
<inspections>
    <errors>    </errors>
    <warnings>
        <warning class = "${ClassHasNoToStringMethodInspection::class.jvmName}"/>
    </warnings>
    <infos>    </infos>
</inspections>
                """
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceJavaFile, "public class Main { private int x = 42; }")

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("--info", "--stacktrace", "inspectionsMain")
                .withPluginClasspath()
                .build()

        println(result.output)
        assertEquals(result.task(":inspectionsMain").outcome, SUCCESS)
    }

    @Test
    fun testInspectionConfigurationKotlin() {
        val buildFileContent =
                """
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.4"
    }
}

plugins {
    id 'java'
    id 'org.jetbrains.kotlin.jvm' version '1.1.4'
    id 'org.jetbrains.intellij.inspections'
}

sourceSets {
    main {
        java {
            srcDirs = ['src']
        }
    }
}
                """
        writeFile(buildFile, buildFileContent)
        val inspectionsFileContent =
                """<?xml version="1.0" encoding="UTF-8" ?>
<inspections>
    <errors>    </errors>
    <warnings>
        <warning class = "${ConvertToStringTemplateInspection::class.jvmName}"/>
    </warnings>
    <infos>    </infos>
</inspections>
                """
        writeFile(inspectionsFile, inspectionsFileContent)
        writeFile(sourceKotlinFile, "fun foo() = \"a\" + \"b\"")

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("--info", "--stacktrace", "inspectionsMain")
                .withPluginClasspath()
                .build()

        println(result.output)
        assertEquals(result.task(":inspectionsMain").outcome, SUCCESS)
    }

    private fun writeFile(destination: File, content: String) {
        destination.bufferedWriter().use {
            it.write(content)
        }
    }
}