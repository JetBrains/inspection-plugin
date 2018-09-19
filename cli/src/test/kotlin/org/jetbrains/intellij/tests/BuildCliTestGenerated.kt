import org.jetbrains.intellij.SettingsBuilder
import org.jetbrains.intellij.inspection.ToolArgumentsBuilder
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class BuildCliTestGenerated {
    private val testBench = InspectionTestBench("checkInspections")

    @Test
    fun testRedundantVisibility() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["Redundant visibility modifier"] = SettingsBuilder.Inspection().apply {
                    quickFix = true
                }
            }
        }
        testBench.doTest(File("testData/build/redundantVisibility"), toolArguments)
    }

    @Test
    fun testSpaces() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.ReformatInspection"] = SettingsBuilder.Inspection().apply {
                    quickFix = true
                }
            }
        }
        testBench.doTest(File("testData/build/spaces"), toolArguments)
    }

    @Test
    fun testUnusedReceiverParameterInspection() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["UnusedReceiverParameter"] = SettingsBuilder.Inspection().apply {
                    quickFix = true
                }
            }
        }
        testBench.doTest(File("testData/build/unusedReceiverParameterInspection"), toolArguments)
    }
}