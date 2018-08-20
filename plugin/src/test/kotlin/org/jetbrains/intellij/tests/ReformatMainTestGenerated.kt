import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class ReformatMainTestGenerated {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private val testBench = InspectionTestBench(testProjectDir, "reformatMain")

    @Ignore
    @Test
    fun testConfigurationKotlin_1_2_60_IJ2017_2() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.idea.version = "ideaIC:2017.2"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/reformat/configurationKotlin_1-2-60_IJ2017-2"), extension)
    }

    @Ignore
    @Test
    fun testConfigurationKotlin_1_2_60_IJ2017_3() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/reformat/configurationKotlin_1-2-60_IJ2017-3"), extension)
    }

    @Ignore
    @Test
    fun testConfigurationKotlin_1_2_60_IJ2018_1() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.idea.version = "ideaIC:2018.1"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/reformat/configurationKotlin_1-2-60_IJ2018-1"), extension)
    }

    @Ignore
    @Test
    fun testConfigurationKotlin_1_2_60_IJ2018_2() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/reformat/configurationKotlin_1-2-60_IJ2018-2"), extension)
    }

    @Test
    fun testMultiFile() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        testBench.doTest(File("testData/reformat/multiFile"), extension)
    }

    @Test
    fun testSpaces() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        testBench.doTest(File("testData/reformat/spaces"), extension)
    }
}