package org.jetbrains.intellij

import org.jetbrains.intellij.inspection.InspectionTestGenerator
import java.io.File

fun main(args: Array<String>) {
    val testsDir = File("src/test/kotlin/org/jetbrains/intellij/tests")
    val testDataDir = File("testData")
    testsDir.mkdirs()
    val testGenerator = InspectionTestGenerator(testsDir, testDataDir)
    testGenerator.generate("checkInspections", "build")
    testGenerator.generate("inspections", "inspection")
    testGenerator.generate("reformat", "reformat")
}