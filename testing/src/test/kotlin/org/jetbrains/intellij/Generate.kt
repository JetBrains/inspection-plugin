package org.jetbrains.intellij

import org.jetbrains.intellij.cli.CliTestGenerator
import org.jetbrains.intellij.plugin.PluginTestGenerator
import java.io.File

fun main(args: Array<String>) {
    val testsDir = File("src/test/kotlin/org/jetbrains/intellij/tests")
    val testDataDir = File("testData")
    testsDir.deleteRecursively()
    testsDir.mkdirs()
    PluginTestGenerator(testsDir, testDataDir).apply {
        generate("build", "build")
        generate("inspectionsMain", "inspection")
        generate("inspectionsMain", "ideaConfiguration")
        generate("reformatMain", "reformat")
        generate("reformatMain", "kotlinConfiguration")
    }
    CliTestGenerator(testsDir, testDataDir).apply {
        generate("checkInspections", "build")
        generate("inspections", "inspection")
        generate("reformat", "reformat")
    }
}