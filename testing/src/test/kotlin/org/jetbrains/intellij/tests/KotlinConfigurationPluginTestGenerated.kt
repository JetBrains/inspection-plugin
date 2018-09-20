import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.plugin.PluginTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class KotlinConfigurationPluginTestGenerated {
    private val testBench = PluginTestBench("reformatMain")

    @Test
    fun testConfigurationKotlin_1_2_40_IJ2017_1() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.1"
        extension.plugins.kotlin.version = "1.2.40"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-40_IJ2017-1"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_40_IJ2017_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.2"
        extension.plugins.kotlin.version = "1.2.40"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-40_IJ2017-2"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_40_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.version = "1.2.40"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-40_IJ2017-3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_40_IJ2018_1() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.1"
        extension.plugins.kotlin.version = "1.2.40"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-40_IJ2018-1"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_40_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.version = "1.2.40"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-40_IJ2018-2"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_51_IJ2017_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.2"
        extension.plugins.kotlin.version = "1.2.51"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-51_IJ2017-2"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_51_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.version = "1.2.51"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-51_IJ2017-3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_51_IJ2018_1() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.1"
        extension.plugins.kotlin.version = "1.2.51"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-51_IJ2018-1"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_51_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.version = "1.2.51"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-51_IJ2018-2"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_60_IJ2017_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.2"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-60_IJ2017-2"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_60_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-60_IJ2017-3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_60_IJ2018_1() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.1"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-60_IJ2018-1"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_60_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/kotlinConfiguration/configurationKotlin_1-2-60_IJ2018-2"), extension)
    }
}