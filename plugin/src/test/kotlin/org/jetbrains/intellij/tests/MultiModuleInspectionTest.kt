package org.jetbrains.intellij.tests

import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import java.io.File

class MultiModuleInspectionTest {
    private val testBench = InspectionTestBench("inspectionsMain")

    @Test
    fun testFirst() {
        val extension = InspectionPluginExtension(null)
        extension.testMode = true
        testBench.doMultiModuleTest(File("testData/multimodule/first"), extension)
    }
}