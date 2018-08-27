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
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection").quickFix = true
        testBench.doTest(File("testData/build/redundantVisibility"), extension)
    }

    @Ignore
    @Test
    fun testSpaces() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReformatInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReformatInspection").quickFix = true
        testBench.doTest(File("testData/build/spaces"), extension)
    }

    @Test
    fun testUnusedReceiverParameterInspection() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection").quickFix = true
        testBench.doTest(File("testData/build/unusedReceiverParameterInspection"), extension)
    }
}