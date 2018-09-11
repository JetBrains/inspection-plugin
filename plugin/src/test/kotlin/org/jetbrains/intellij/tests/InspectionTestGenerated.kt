import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class InspectionTestGenerated {
    private val testBench = InspectionTestBench("inspectionsMain")

    @Test
    fun testAddVariance() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection")
        testBench.doTest(File("testData/inspection/addVariance"), extension)
    }

    @Test
    fun testConfigurationIdea_Default() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/inspection/configurationIdea_Default"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2017_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.2"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/inspection/configurationIdea_IJ2017_2"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/inspection/configurationIdea_IJ2017_3"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2018_1() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.1"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/inspection/configurationIdea_IJ2018_1"), extension)
    }

    @Test
    fun testConfigurationIdea_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/inspection/configurationIdea_IJ2018_2"), extension)
    }

    @Test
    fun testConfigurationIdea_IU2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIU:2017.3"
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/inspection/configurationIdea_IU2017_3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_IJ2017_3_with_idea_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49053"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/inspection/configurationKotlin_1-2-61_IJ2017-3_with_idea_IJ2018-2"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_IJ2018_2_with_idea_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49055"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/inspection/configurationKotlin_1-2-61_IJ2018-2_with_idea_IJ2017-3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_Studio_with_idea_IJ2017_3() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2017.3"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49186"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/inspection/configurationKotlin_1-2-61_Studio_with_idea_IJ2017-3"), extension)
    }

    @Test
    fun testConfigurationKotlin_1_2_61_Studio_with_idea_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.idea.version = "ideaIC:2018.2"
        extension.plugins.kotlin.location = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49186"
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection")
        testBench.doTest(File("testData/inspection/configurationKotlin_1-2-61_Studio_with_idea_IJ2018-2"), extension)
    }

    @Test
    fun testConvertToStringTemplate() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateInspection")
        testBench.doTest(File("testData/inspection/convertToStringTemplate"), extension)
    }

    @Test
    fun testCustomConfigInheritFromIdea() {
        val extension = InspectionPluginExtension(null)
        extension.inheritFromIdea = true
        testBench.doTest(File("testData/inspection/customConfigInheritFromIdea"), extension)
    }

    @Test
    fun testDoNotShowViolations() {
        val extension = InspectionPluginExtension(null)
        extension.isQuiet = true
        extension.warning("org.jetbrains.kotlin.idea.inspections.CanBeParameterInspection")
        testBench.doTest(File("testData/inspection/doNotShowViolations"), extension)
    }

    @Test
    fun testHtmlOutput() {
        val extension = InspectionPluginExtension(null)
        extension.error("org.jetbrains.kotlin.idea.inspections.CanBeValInspection")
        extension.errors.max = 1000
        extension.warning("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection")
        extension.info("org.jetbrains.kotlin.idea.intentions.FoldInitializerAndIfToElvisInspection")
        testBench.doTest(File("testData/inspection/htmlOutput"), extension)
    }

    @Test
    fun testJavaInspections() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection")
        testBench.doTest(File("testData/inspection/javaInspections"), extension)
    }

    @Test
    fun testMaxErrors() {
        val extension = InspectionPluginExtension(null)
        extension.error("org.jetbrains.kotlin.idea.inspections.CanBeValInspection")
        extension.errors.max = 2
        testBench.doTest(File("testData/inspection/maxErrors"), extension)
    }

    @Test
    fun testMaxWarningsIgnoreFailures() {
        val extension = InspectionPluginExtension(null)
        extension.isIgnoreFailures = true
        extension.warning("org.jetbrains.kotlin.idea.inspections.CanBeValInspection")
        extension.warnings.max = 2
        testBench.doTest(File("testData/inspection/maxWarningsIgnoreFailures"), extension)
    }

    @Test
    fun testPluginInjection() {
        val extension = InspectionPluginExtension(null)
        extension.plugins.kotlin.version = "1.2.60"
        extension.warning("org.jetbrains.kotlin.idea.inspections.CanSealedSubClassBeObjectInspection")
        testBench.doTest(File("testData/inspection/pluginInjection"), extension)
    }

    @Test
    fun testRedundantModality() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantModalityModifierInspection")
        testBench.doTest(File("testData/inspection/redundantModality"), extension)
    }

    @Test
    fun testRedundantVisibility() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection").quickFix = true
        testBench.doTest(File("testData/inspection/redundantVisibility"), extension)
    }

    @Test
    fun testSpaces() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReformatInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.ReformatInspection").quickFix = true
        testBench.doTest(File("testData/inspection/spaces"), extension)
    }

    @Test
    fun testStdlib() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.KotlinCleanupInspection")
        testBench.doTest(File("testData/inspection/stdlib"), extension)
    }

    @Test
    fun testUnusedReceiverParameterInspection() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection")
        extension.warning("org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection").quickFix = true
        testBench.doTest(File("testData/inspection/unusedReceiverParameterInspection"), extension)
    }

    @Test
    fun testUnusedSymbolByIdeaProfile() {
        val extension = InspectionPluginExtension(null)
        extension.inheritFromIdea = true
        extension.error("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        extension.errors.max = 2
        testBench.doTest(File("testData/inspection/unusedSymbolByIdeaProfile"), extension)
    }

    @Test
    fun testUnusedSymbolError() {
        val extension = InspectionPluginExtension(null)
        extension.error("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        testBench.doTest(File("testData/inspection/unusedSymbolError"), extension)
    }

    @Test
    fun testUnusedSymbolIdeaError() {
        val extension = InspectionPluginExtension(null)
        extension.inheritFromIdea = true
        extension.error("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaError"), extension)
    }

    @Ignore
    @Test
    fun testUnusedSymbolIdeaError_IJ2018_2() {
        val extension = InspectionPluginExtension(null)
        extension.inheritFromIdea = true
        extension.idea.version = "ideaIC:2018.2"
        extension.error("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaError_IJ2018_2"), extension)
    }

    @Test
    fun testUnusedSymbolIdeaWarning() {
        val extension = InspectionPluginExtension(null)
        extension.inheritFromIdea = true
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaWarning"), extension)
    }

    @Test
    fun testUnusedSymbolWarning() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        testBench.doTest(File("testData/inspection/unusedSymbolWarning"), extension)
    }

    @Test
    fun testWeakWarningNeverBecomesError() {
        val extension = InspectionPluginExtension(null)
        extension.error("org.jetbrains.kotlin.idea.inspections.LeakingThisInspection")
        testBench.doTest(File("testData/inspection/weakWarningNeverBecomesError"), extension)
    }

    @Test
    fun testXmlOutput() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection")
        testBench.doTest(File("testData/inspection/xmlOutput"), extension)
    }
}