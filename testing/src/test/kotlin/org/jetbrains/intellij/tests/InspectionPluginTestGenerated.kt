import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.plugin.PluginTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class InspectionPluginTestGenerated {
    private val testBench = PluginTestBench("inspectionsMain")

    @Test
    fun testAddVariance() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection")
        testBench.doTest(File("testData/inspection/addVariance"), extension)
    }

    @Test
    fun testConvertToStringTemplate() {
        val extension = InspectionPluginExtension(null)
        extension.warning("ConvertToStringTemplate")
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
        extension.warning("ClassHasNoToStringMethod")
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
    fun testRedundantModality() {
        val extension = InspectionPluginExtension(null)
        extension.warning("org.jetbrains.kotlin.idea.inspections.RedundantModalityModifierInspection")
        testBench.doTest(File("testData/inspection/redundantModality"), extension)
    }

    @Test
    fun testRedundantVisibility() {
        val extension = InspectionPluginExtension(null)
        extension.warning("RedundantVisibilityModifier")
        extension.warning("RedundantVisibilityModifier").quickFix = true
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

    @Ignore
    @Test
    fun testUnusedDeclaration() {
        val extension = InspectionPluginExtension(null)
        extension.warning("UnusedDeclaration")
        testBench.doTest(File("testData/inspection/unusedDeclaration"), extension)
    }

    @Test
    fun testUnusedImport() {
        val extension = InspectionPluginExtension(null)
        extension.warning("UnusedImport")
        testBench.doTest(File("testData/inspection/unusedImport"), extension)
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
        extension.error("UnusedSymbol")
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
        extension.error("UnusedSymbol")
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaError"), extension)
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