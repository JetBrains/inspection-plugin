import org.jetbrains.intellij.SettingsBuilder
import org.jetbrains.intellij.cli.ToolArgumentsBuilder
import org.jetbrains.intellij.cli.CliTestBench
import org.junit.Test
import org.junit.Ignore
import java.io.File

class InspectionCliTestGenerated {
    private val testBench = CliTestBench("inspections")

    @Test
    fun testAddVariance() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/addVariance"), toolArguments)
    }

    @Test
    fun testConvertToStringTemplate() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["ConvertToStringTemplate"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/convertToStringTemplate"), toolArguments)
    }

    @Test
    fun testCustomConfigInheritFromIdea() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                inheritFromIdea = true
            }
        }
        testBench.doTest(File("testData/inspection/customConfigInheritFromIdea"), toolArguments)
    }

    @Test
    fun testDoNotShowViolations() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                report.isQuiet = true
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.CanBeParameterInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/doNotShowViolations"), toolArguments)
    }

    @Test
    fun testHtmlOutput() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                errors.inspections["org.jetbrains.kotlin.idea.inspections.CanBeValInspection"] = SettingsBuilder.Inspection()
                errors.max = 1000
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"] = SettingsBuilder.Inspection()
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection"] = SettingsBuilder.Inspection()
                info.inspections["org.jetbrains.kotlin.idea.intentions.FoldInitializerAndIfToElvisInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/htmlOutput"), toolArguments)
    }

    @Test
    fun testJavaInspections() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["ClassHasNoToStringMethod"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/javaInspections"), toolArguments)
    }

    @Test
    fun testMaxErrors() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                errors.inspections["org.jetbrains.kotlin.idea.inspections.CanBeValInspection"] = SettingsBuilder.Inspection()
                errors.max = 2
            }
        }
        testBench.doTest(File("testData/inspection/maxErrors"), toolArguments)
    }

    @Test
    fun testMaxWarningsIgnoreFailures() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                ignoreFailures = true
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.CanBeValInspection"] = SettingsBuilder.Inspection()
                warnings.max = 2
            }
        }
        testBench.doTest(File("testData/inspection/maxWarningsIgnoreFailures"), toolArguments)
    }

    @Test
    fun testRedundantModality() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.RedundantModalityModifierInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/redundantModality"), toolArguments)
    }

    @Test
    fun testRedundantVisibility() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["RedundantVisibilityModifier"] = SettingsBuilder.Inspection().apply {
                    quickFix = true
                }
            }
        }
        testBench.doTest(File("testData/inspection/redundantVisibility"), toolArguments)
    }

    @Test
    fun testSpaces() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.ReformatInspection"] = SettingsBuilder.Inspection().apply {
                    quickFix = true
                }
            }
        }
        testBench.doTest(File("testData/inspection/spaces"), toolArguments)
    }

    @Test
    fun testStdlib() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"] = SettingsBuilder.Inspection()
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.KotlinCleanupInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/stdlib"), toolArguments)
    }

    @Ignore
    @Test
    fun testUnusedDeclaration() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["UnusedDeclaration"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/unusedDeclaration"), toolArguments)
    }

    @Test
    fun testUnusedImport() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["UnusedImport"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/unusedImport"), toolArguments)
    }

    @Test
    fun testUnusedReceiverParameterInspection() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.UnusedReceiverParameterInspection"] = SettingsBuilder.Inspection().apply {
                    quickFix = true
                }
            }
        }
        testBench.doTest(File("testData/inspection/unusedReceiverParameterInspection"), toolArguments)
    }

    @Test
    fun testUnusedSymbolByIdeaProfile() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                inheritFromIdea = true
                errors.inspections["UnusedSymbol"] = SettingsBuilder.Inspection()
                errors.max = 2
            }
        }
        testBench.doTest(File("testData/inspection/unusedSymbolByIdeaProfile"), toolArguments)
    }

    @Test
    fun testUnusedSymbolError() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                errors.inspections["org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/unusedSymbolError"), toolArguments)
    }

    @Test
    fun testUnusedSymbolIdeaError() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                inheritFromIdea = true
                errors.inspections["UnusedSymbol"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaError"), toolArguments)
    }

    @Ignore
    @Test
    fun testUnusedSymbolIdeaError_IJ2018_2() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                inheritFromIdea = true
                errors.inspections["org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaError_IJ2018_2"), toolArguments)
    }

    @Test
    fun testUnusedSymbolIdeaWarning() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                inheritFromIdea = true
            }
        }
        testBench.doTest(File("testData/inspection/unusedSymbolIdeaWarning"), toolArguments)
    }

    @Test
    fun testUnusedSymbolWarning() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/unusedSymbolWarning"), toolArguments)
    }

    @Test
    fun testWeakWarningNeverBecomesError() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                errors.inspections["org.jetbrains.kotlin.idea.inspections.LeakingThisInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/weakWarningNeverBecomesError"), toolArguments)
    }

    @Test
    fun testXmlOutput() {
        val toolArguments = ToolArgumentsBuilder().apply {
            settings = SettingsBuilder().apply {
                warnings.inspections["org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection"] = SettingsBuilder.Inspection()
            }
        }
        testBench.doTest(File("testData/inspection/xmlOutput"), toolArguments)
    }
}