import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.plugin.PluginTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class IdeaConfigurationPluginTestGenerated {
    private val testBench = PluginTestBench("inspectionsMain")

    @Test
    fun testConfigurationIdea_Default() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationIdea_Default"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2017_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.2"
        extension.warning("RedundantVisibilityModifier")
        testBench.doTest(File("testData/ideaConfiguration/configurationIdea_IJ2017_2"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationIdea_IJ2017_3"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2018_1() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.1"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationIdea_IJ2018_1"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationIdea_IJ2018_2"), extension)
    }

    @Test
    fun testConfigurationIdea_IU2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIU:2017.3"
        extension.warning("RedundantVisibilityModifier")
        testBench.doTest(File("testData/ideaConfiguration/configurationIdea_IU2017_3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_IJ2017_3_with_idea_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49053"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationKotlin_1-2-61_IJ2017-3_with_idea_IJ2018-2"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_IJ2018_2_with_idea_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49055"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationKotlin_1-2-61_IJ2018-2_with_idea_IJ2017-3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_Studio_with_idea_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49186"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationKotlin_1-2-61_Studio_with_idea_IJ2017-3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_Studio_with_idea_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49186"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/ideaConfiguration/configurationKotlin_1-2-61_Studio_with_idea_IJ2018-2"), extension)
    }

    @Test
    fun testPluginInjection() {
        val extension = InspectionPluginExtension(null)
        extension.plugins.kotlin.version = "1.2.60"
        extension.warning("org.jetbrains.kotlin.idea.inspections.CanSealedSubClassBeObjectInspection")
        testBench.doTest(File("testData/ideaConfiguration/pluginInjection"), extension)
    }
}