import org.jetbrains.intellij.inspection.ToolArguments
import org.jetbrains.intellij.inspection.InspectionTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class InspectionTestGenerated {
    private val testBench = InspectionTestBench("inspections")

    @Test
    fun testAddVariance() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/addVariance"), toolArguments)
    }

    @Test
    fun testConfigurationIdea_Default() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/configurationIdea_Default"), toolArguments)
    }

    @Test
    fun testConvertToStringTemplate() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("ConvertToStringTemplate"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/convertToStringTemplate"), toolArguments)
    }

    @Test
    fun testCustomConfigInheritFromIdea() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/customConfigInheritFromIdea"), toolArguments)
    }

    @Test
    fun testDoNotShowViolations() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.CanBeParameterInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/doNotShowViolations"), toolArguments)
    }

    @Test
    fun testHtmlOutput() {
        val toolArguments = ToolArguments(
            errors = listOf("org.jetbrains.kotlin.idea.inspections.CanBeValInspection"),
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection", "org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"),
            info = listOf("org.jetbrains.kotlin.idea.intentions.FoldInitializerAndIfToElvisInspection"),
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/htmlOutput"), toolArguments)
    }

    @Test
    fun testJavaInspections() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("ClassHasNoToStringMethod"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/javaInspections"), toolArguments)
    }

    @Test
    fun testMaxErrors() {
        val toolArguments = ToolArguments(
            errors = listOf("org.jetbrains.kotlin.idea.inspections.CanBeValInspection"),
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/maxErrors"), toolArguments)
    }

    @Test
    fun testMaxWarningsIgnoreFailures() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.CanBeValInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/maxWarningsIgnoreFailures"), toolArguments)
    }

    @Test
    fun testPluginInjection() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.CanSealedSubClassBeObjectInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/pluginInjection"), toolArguments)
    }

    @Test
    fun testRedundantModality() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.RedundantModalityModifierInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/redundantModality"), toolArguments)
    }

    @Test
    fun testRedundantVisibility() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("RedundantVisibilityModifier"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/redundantVisibility"), toolArguments)
    }

    @Test
    fun testSpaces() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.ReformatInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/spaces"), toolArguments)
    }

    @Test
    fun testStdlib() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.KotlinCleanupInspection", "org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/stdlib"), toolArguments)
    }

    @Test
    fun testUnusedReceiverParameterInspection() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/unusedReceiverParameterInspection"), toolArguments)
    }

    @Test
    fun testUnusedSymbolByIdeaProfile() {
        val toolArguments = ToolArguments(
            errors = listOf("UnusedSymbol"),
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/unusedSymbolByIdeaProfile"), toolArguments)
    }

    @Test
    fun testUnusedSymbolError() {
        val toolArguments = ToolArguments(
            errors = listOf("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"),
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/unusedSymbolError"), toolArguments)
    }

    @Test
    fun testUnusedSymbolIdeaError() {
        val toolArguments = ToolArguments(
            errors = listOf("UnusedSymbol"),
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaError"), toolArguments)
    }

    @Test
    fun testUnusedSymbolIdeaWarning() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaWarning"), toolArguments)
    }

    @Test
    fun testUnusedSymbolWarning() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/unusedSymbolWarning"), toolArguments)
    }

    @Test
    fun testWeakWarningNeverBecomesError() {
        val toolArguments = ToolArguments(
            errors = listOf("org.jetbrains.kotlin.idea.inspections.LeakingThisInspection"),
            warnings = null,
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/weakWarningNeverBecomesError"), toolArguments)
    }

    @Test
    fun testXmlOutput() {
        val toolArguments = ToolArguments(
            errors = null,
            warnings = listOf("org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection"),
            info = null,
            tasks = null,
            level = null,
            config = null,
            runner = null,
            idea = null,
            project = null
        )
        testBench.doTest(File("testData/inspection/xmlOutput"), toolArguments)
    }
}