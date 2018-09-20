import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.plugin.PluginTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class ReformatPluginTestGenerated {
    private val testBench = PluginTestBench("reformatMain")

    @Test
    fun testMultiFile() {
        val extension = InspectionPluginExtension(null)
        testBench.doTest(File("testData/reformat/multiFile"), extension)
    }

    @Test
    fun testSpaces() {
        val extension = InspectionPluginExtension(null)
        testBench.doTest(File("testData/reformat/spaces"), extension)
    }
}