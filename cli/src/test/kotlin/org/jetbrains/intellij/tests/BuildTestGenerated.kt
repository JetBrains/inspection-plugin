import org.jetbrains.intellij.inspection.ToolArguments
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class BuildTestGenerated {
    private val testBench = InspectionTestBench("checkInspections")

    @Test
    fun testRedundantVisibility() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("Redundant visibility modifier"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/build/redundantVisibility"), toolArguments)
    }

    @Test
    fun testSpaces() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.ReformatInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/build/spaces"), toolArguments)
    }

    @Test
    fun testUnusedReceiverParameterInspection() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("UnusedReceiverParameter"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/build/unusedReceiverParameterInspection"), toolArguments)
    }
}