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

    @Test
    fun testConfigurationKotlin2017_2() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.kotlinPluginVersion = "1.2.60-release-IJ2017.2-1"
        testBench.doTest(File("testData\\reformat\\configurationKotlin2017_2"), extension)
    }

    @Test
    fun testConfigurationKotlin2017_3() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.kotlinPluginVersion = "1.2.60-release-IJ2017.3-1"
        testBench.doTest(File("testData\\reformat\\configurationKotlin2017_3"), extension)
    }

    @Test
    fun testConfigurationKotlin2018_1() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.kotlinPluginVersion = "1.2.60-release-IJ2018.1-1"
        testBench.doTest(File("testData\\reformat\\configurationKotlin2018_1"), extension)
    }

    @Test
    fun testConfigurationKotlin2018_2() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.kotlinPluginVersion = "1.2.60-release-IJ2018.2-1"
        testBench.doTest(File("testData\\reformat\\configurationKotlin2018_2"), extension)
    }

    @Test
    fun testConfigurationKotlinStudio3_3c4_1() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        extension.kotlinPluginVersion = "1.2.60-release-76-Studio3.3c4-1"
        extension.kotlinPluginLocation = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=48589"
        testBench.doTest(File("testData\\reformat\\configurationKotlinStudio3_3c4_1"), extension)
    }

    @Test
    fun testMultiFile() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        testBench.doTest(File("testData\\reformat\\multiFile"), extension)
    }

    @Test
    fun testSpaces() {
        val extension = InspectionsExtension(null)
        extension.testMode = true
        testBench.doTest(File("testData\\reformat\\spaces"), extension)
    }
}