import org.jetbrains.intellij.inspection.ToolArguments
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class ReformatTestGenerated {
    private val testBench = InspectionTestBench("reformat")

    @Test
    fun testMultiFile() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null,
            html = null,
            xml = null
        )
        testBench.doTest(File("testData/reformat/multiFile"), toolArguments)
    }

    @Test
    fun testSpaces() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null,
            html = null,
            xml = null
        )
        testBench.doTest(File("testData/reformat/spaces"), toolArguments)
    }
}