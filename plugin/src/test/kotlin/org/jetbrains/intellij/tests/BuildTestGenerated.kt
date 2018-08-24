import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class BuildTestGenerated {
    private val testBench = InspectionTestBench("build")

    @Test
    fun testRedundantVisibility() {
        val extension = InspectionPluginExtension(null)
        extension.warnings.inspection("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.warnings.inspection("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection").quickFix = true
        testBench.doTest(File("testData/build/redundantVisibility"), extension)
    }

    @Test
    fun testSpaces() {
        val extension = InspectionPluginExtension(null)
        extension.warnings.inspection("org.jetbrains.kotlin.idea.inspections.ReformatInspection")
        extension.warnings.inspection("org.jetbrains.kotlin.idea.inspections.ReformatInspection").quickFix = true
        testBench.doTest(File("testData/build/spaces"), extension)
    }

    @Test
    fun testUnusedReceiverParameterInspection() {
        val extension = InspectionPluginExtension(null)
        extension.warnings.inspection("org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection")
        extension.warnings.inspection("org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection").quickFix = true
        testBench.doTest(File("testData/build/unusedReceiverParameterInspection"), extension)
    }
}