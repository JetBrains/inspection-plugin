package org.jetbrains.intellij

import org.jetbrains.intellij.inspection.InspectionTestGenerator
import java.io.File

fun main(args: Array<String>) {
    val testsDir = File("plugin/src/test/kotlin/org/jetbrains/intellij/tests")
    val testDataDir = File("plugin/testData")
    val testGenerator = InspectionTestGenerator(testsDir, testDataDir)
    testGenerator.generate("inspectionsMain", "inspection")
    testGenerator.generate("reformatMain", "reformat")
}