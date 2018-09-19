import org.jetbrains.intellij.SettingsBuilder
import org.jetbrains.intellij.inspection.ToolArgumentsBuilder
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class ReformatCliTestGenerated {
    private val testBench = InspectionTestBench("reformat")

    @Test
    fun testMultiFile() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder()
        }
        testBench.doTest(File("testData/reformat/multiFile"), toolArguments)
    }

    @Test
    fun testSpaces() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder()
        }
        testBench.doTest(File("testData/reformat/spaces"), toolArguments)
    }
}