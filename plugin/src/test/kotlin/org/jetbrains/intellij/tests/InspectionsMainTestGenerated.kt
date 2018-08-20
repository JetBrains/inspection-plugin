import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class InspectionsMainTestGenerated {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    private val testBench = InspectionTestBench(testProjectDir, "inspectionsMain")

    @Test
    fun testAddVariance() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection")
        testBench.doTest(File("testData/inspection/addVariance"), extension)
    }

    @Test
    fun testConfigurationJava() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection")
        testBench.doTest(File("testData/inspection/configurationJava"), extension)
    }

    @Test
    fun testConfigurationKotlin() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        testBench.doTest(File("testData/inspection/configurationKotlin"), extension)
    }

    @Ignore
    @Test
    fun testConfigurationKotlin2017_2() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.idea.version = "ideaIC:2017.2"
        testBench.doTest(File("testData/inspection/configurationKotlin2017_2"), extension)
    }

    @Ignore
    @Test
    fun testConfigurationKotlin2018_1() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.idea.version = "ideaIC:2018.1"
        testBench.doTest(File("testData/inspection/configurationKotlin2018_1"), extension)
    }

    @Ignore
    @Test
    fun testConfigurationKotlin2018_2() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.idea.version = "ideaIC:2018.2"
        testBench.doTest(File("testData/inspection/configurationKotlin2018_2"), extension)
    }

    @Ignore
    @Test
    fun testConfigurationKotlinUltimate() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.idea.version = "ideaIU:2017.3"
        testBench.doTest(File("testData/inspection/configurationKotlinUltimate"), extension)
    }

    @Test
    fun testConvertToStringTemplate() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateInspection")
        testBench.doTest(File("testData/inspection/convertToStringTemplate"), extension)
    }

    @Test
    fun testCustomConfigInheritFromIdea() {
        val extension = InspectionsExtension(null)
        extension.inheritFromIdea = true
        testBench.doTest(File("testData/inspection/customConfigInheritFromIdea"), extension)
    }

    @Test
    fun testDoNotShowViolations() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.CanBeParameterInspection")
        extension.isQuiet = true
        testBench.doTest(File("testData/inspection/doNotShowViolations"), extension)
    }

    @Test
    fun testHtmlOutput() {
        val extension = InspectionsExtension(null)
        extension.errors.inspections = setOf("org.jetbrains.kotlin.idea.inspections.CanBeValInspection")
        extension.errors.max = 1000
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection", "org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        extension.infos.inspections = setOf("org.jetbrains.kotlin.idea.intentions.FoldInitializerAndIfToElvisInspection")
        testBench.doTest(File("testData/inspection/htmlOutput"), extension)
    }

    @Test
    fun testMaxErrors() {
        val extension = InspectionsExtension(null)
        extension.errors.inspections = setOf("org.jetbrains.kotlin.idea.inspections.CanBeValInspection")
        extension.errors.max = 2
        testBench.doTest(File("testData/inspection/maxErrors"), extension)
    }

    @Test
    fun testMaxWarningsIgnoreFailures() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.CanBeValInspection")
        extension.warnings.max = 2
        extension.isIgnoreFailures = true
        testBench.doTest(File("testData/inspection/maxWarningsIgnoreFailures"), extension)
    }

    @Test
    fun testPluginInjection() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.CanSealedSubClassBeObjectInspection")
        extension.plugins.kotlin.version = "1.2.60"
        testBench.doTest(File("testData/inspection/pluginInjection"), extension)
    }

    @Test
    fun testRedundantModality() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.RedundantModalityModifierInspection")
        testBench.doTest(File("testData/inspection/redundantModality"), extension)
    }

    @Test
    fun testRedundantVisibility() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection")
        extension.quickFix = true
        testBench.doTest(File("testData/inspection/redundantVisibility"), extension)
    }

    @Test
    fun testSpaces() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.ReformatInspection")
        extension.quickFix = true
        testBench.doTest(File("testData/inspection/spaces"), extension)
    }

    @Test
    fun testStdlib() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.KotlinCleanupInspection", "org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        testBench.doTest(File("testData/inspection/stdlib"), extension)
    }

    @Test
    fun testUnusedSymbolByIdeaProfile() {
        val extension = InspectionsExtension(null)
        extension.inheritFromIdea = true
        extension.errors.inspections = setOf("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection")
        extension.errors.max = 2
        testBench.doTest(File("testData/inspection/unusedSymbolByIdeaProfile"), extension)
    }

    @Test
    fun testWeakWarningNeverBecomesError() {
        val extension = InspectionsExtension(null)
        extension.errors.inspections = setOf("org.jetbrains.kotlin.idea.inspections.LeakingThisInspection")
        testBench.doTest(File("testData/inspection/weakWarningNeverBecomesError"), extension)
    }

    @Test
    fun testXmlOutput() {
        val extension = InspectionsExtension(null)
        extension.warnings.inspections = setOf("org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection")
        testBench.doTest(File("testData/inspection/xmlOutput"), extension)
    }
}