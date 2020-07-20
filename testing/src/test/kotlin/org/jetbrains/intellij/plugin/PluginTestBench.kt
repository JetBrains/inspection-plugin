package org.jetbrains.intellij.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome as GradleTaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jetbrains.intellij.*
import org.jetbrains.intellij.TestBench.TaskOutcome as BenchTaskOutcome
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.junit.Assert
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

class PluginTestBench(private val taskName: String) : TestBench<InspectionPluginExtension>() {

    private val testProjectDir = TemporaryFolder(File(System.getProperty("java.io.tmpdir")))

    private fun generateBuildFile(
            kotlinNeeded: Boolean,
            extension: InspectionPluginExtension,
            xmlReport: Boolean = false,
            htmlReport: Boolean = false,
            kotlinVersion: String = "1.2.41"
    ): String {
        val templateLines = File("testData/build.gradle.template").readLines()
        return with(extension) {
            StringBuilder().apply {
                for (line in templateLines) {
                    val template = Regex("<.*>").find(line)
                    if (template == null) {
                        appendln(line)
                        continue
                    }
                    @Suppress("UNNECESSARY_SAFE_CALL")
                    when (template.value.drop(1).dropLast(1)) {
                        "kotlinGradleDependency" -> if (kotlinNeeded) {
                            appendln("        classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion\"")
                        }
                        "kotlinPlugin" -> if (kotlinNeeded) {
                            appendln("    id 'org.jetbrains.kotlin.jvm' version '$kotlinVersion'")
                        }
                        "inheritFromIdea" -> inheritFromIdea?.gradleCode?.let {
                            appendln("    inheritFromIdea $it")
                        }
                        "profileName" -> profileName?.gradleCode?.let {
                            appendln("    profileName $it")
                        }
                        "errors.inspections" -> errors.inspections?.forEach { entry ->
                            val name = entry.key.gradleCode
                            if (inheritFromIdea != true) appendln("    error($name)")
                            entry.value.quickFix?.gradleCode?.let {
                                appendln("    error($name).quickFix $it")
                            }
                        }
                        "warnings.inspections" -> warnings.inspections?.forEach { entry ->
                            val name = entry.key.gradleCode
                            if (inheritFromIdea != true) appendln("    warning($name)")
                            entry.value.quickFix?.gradleCode?.let {
                                appendln("    warning($name).quickFix $it")
                            }
                        }
                        "info.inspections" -> info.inspections?.forEach { entry ->
                            val name = entry.key.gradleCode
                            if (inheritFromIdea != true) appendln("    info($name)")
                            entry.value.quickFix?.gradleCode?.let {
                                appendln("    info($name).quickFix $it")
                            }
                        }
                        "errors.max" -> errors.max?.gradleCode?.let {
                            appendln("    errors.max $it")
                        }
                        "warnings.max" -> warnings.max?.gradleCode?.let {
                            appendln("    warnings.max $it")
                        }
                        "info.max" -> info.max?.gradleCode?.let {
                            appendln("    info.max $it")
                        }
                        "quiet" -> isQuiet?.gradleCode?.let {
                            appendln("    quiet $it")
                        }
                        "reformat.quickFix" -> reformat.quickFix?.gradleCode?.let {
                            appendln("    reformat.quickFix $it")
                        }
                        "ignoreFailures" -> isIgnoreFailures?.gradleCode?.let {
                            appendln("    ignoreFailures $it")
                        }
                        "idea.version" -> idea.version?.gradleCode?.let {
                            appendln("    idea.version $it")
                        }
                        "plugins.kotlin.version" -> plugins.kotlin.version?.gradleCode?.let {
                            appendln("    plugins.kotlin.version $it")
                        }
                        "plugins.kotlin.location" -> plugins.kotlin.location?.gradleCode?.let {
                            appendln("    plugins.kotlin.location $it")
                        }
                        "xmlDestination" -> if (xmlReport) {
                            appendln("            destination file(\"build/report.xml\")")
                        }
                        "htmlDestination" -> if (htmlReport) {
                            appendln("            destination file(\"build/report.html\")")
                        }
                        "kotlin-stdlib" -> if (kotlinNeeded) {
                            appendln("    compile \"org.jetbrains.kotlin:kotlin-stdlib\"")
                        }
                        "compile-kotlin" -> if (kotlinNeeded) {
                            appendln("""
                                compileKotlin {
                                    kotlinOptions.jvmTarget = "1.8"
                                }
                                compileTestKotlin {
                                    kotlinOptions.jvmTarget = "1.8"
                                }
                                """.trimIndent()
                            )
                        }
                    }
                }
                println(this)
            }
        }.toString()
    }

    private fun writeFile(destination: File, content: String) {
        destination.bufferedWriter().use {
            it.write(content)
        }
    }

    private fun generateInspectionToolTags(
            level: String,
            inspections: List<String>
    ): String {
        return StringBuilder().apply {
            for (inspectionClass in inspections) {
                val shortClass = inspectionClass.substringAfterLast(".").substringBefore("Inspection")
                appendln("""        <inspection_tool class="$shortClass" enabled="true" level="$level" enabled_by_default="true"/>""")
            }
        }.toString()

    }

    private fun generateInspectionProfileFile(errors: List<String>, warnings: List<String>, info: List<String>): String {
        return StringBuilder().apply {
            appendln("<component name=\"InspectionProjectProfileManager\">")
            appendln("    <profile version=\"1.0\">\n")
            appendln("        <option name=\"myName\" value=\"Project Default\" /> \n")
            appendln(generateInspectionToolTags("ERROR", errors))
            appendln(generateInspectionToolTags("WARNING", warnings))
            appendln(generateInspectionToolTags("INFO", info))
            appendln("    </profile>")
            appendln("</component>")
        }.toString()
    }

    private inner class InspectionTestConfiguration(
            val testDir: File,
            val sources: List<File>,
            val expectedSources: List<File>,
            val extension: InspectionPluginExtension,
            val xmlReport: Boolean,
            val htmlReport: Boolean,
            val kotlinVersion: String,
            val expectedOutcome: GradleTaskOutcome,
            val expectedException: String?,
            val expectedDiagnosticsStatus: DiagnosticsStatus,
            vararg val expectedDiagnostics: String
    ) {
        fun doTest() {
            testProjectDir.create()
            try {
                testProjectDir.newFolder("build")
                initTestProjectIdeaProfile()
                initTestProjectBuildFile()
                val testFiles = initTestProjectSources()
                val result = buildTestProject()
                assertInspectionBuildLog(result)
                assertInspectionBuildXmlReport()
                assertInspectionBuildHtmlReport()
                assertInspectionBuildProjectFiles(testFiles)
            } finally {
                testProjectDir.delete()
            }
        }

        private fun initTestProjectIdeaProfile() {
            with(extension) {
                val errors = errors.inspections.keys.toList()
                val warnings = warnings.inspections.keys.toList()
                val info = info.inspections.keys.toList()
                if (inheritFromIdea == true && (errors + warnings + info).isNotEmpty()) {
                    if (profileName != null)
                        throw IllegalArgumentException("Idea profile in inspection test has auto generating")
                    testProjectDir.newFolder(".idea", "inspectionProfiles")
                    val profileName = "Project_Default.xml"
                    val inspectionProfileFile = testProjectDir.newFile(".idea/inspectionProfiles/$profileName")
                    val inspectionProfileContent = generateInspectionProfileFile(errors, warnings, info)
                    println(inspectionProfileContent)
                    writeFile(inspectionProfileFile, inspectionProfileContent)
                }
            }
        }

        private fun initTestProjectBuildFile() {
            val buildFile = testProjectDir.newFile("build.gradle")
            val hasKotlinFiles = sources.find { it.isKotlinSource } != null
            val buildFileContent = generateBuildFile(
                    hasKotlinFiles,
                    extension,
                    xmlReport,
                    htmlReport,
                    kotlinVersion
            )
            writeFile(buildFile, buildFileContent)
        }

        private fun initTestProjectSources(): List<File> {
            testProjectDir.newFolder("src", "main", "kotlin")
            testProjectDir.newFolder("src", "main", "java")
            val testFiles = ArrayList<File>()
            for (source in sources) {
                val file = source.relativeTo(testDir)
                val file2 = file.toMavenLayout()
                val dir2 = File(testProjectDir.root, file2.parentFile.path)
                if (!dir2.exists()) dir2.mkdirs()
                val file3 = testProjectDir.newFile(file2.path)
                testFiles.add(file3)
            }
            sources.zip(testFiles).forEach { it.first.copyTo(it.second, overwrite = true) }
            return testFiles
        }

        private fun buildTestProject(): BuildResult = try {
            val workingDirectory = System.getProperty("user.dir")
            val inspectionPluginProjectDirectory = File(workingDirectory, "..").canonicalFile
            val pluginUnderTestMetadata = File(
                    inspectionPluginProjectDirectory,
                    "plugin/build/pluginUnderTestMetadata/plugin-under-test-metadata.properties"
            )
            val pluginClasspath = PluginUnderTestMetadataReading.readImplementationClasspath(
                    pluginUnderTestMetadata.toURI().toURL()
            ) + listOf(
                    File(inspectionPluginProjectDirectory, "interface/build/classes/kotlin/main"),
                    File(inspectionPluginProjectDirectory, "frontend/build/classes/kotlin/main")
            ).map {
                it.absoluteFile
            }
            GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments("--no-daemon")
                    .withArguments("--info", "--stacktrace", taskName)
                    // This applies classpath from pluginUnderTestMetadata
                    // NB: this is necessary to apply actual plugin
                    .withPluginClasspath(pluginClasspath)
                    .forwardOutput()
                    .build()
        } catch (failure: UnexpectedBuildFailure) {
            println("PluginTestBench: Exception caught in test.")
            failure.buildResult
        }

        private fun assertInspectionBuildLog(buildResult: BuildResult) {
            for (diagnostic in expectedDiagnostics) {
                when (expectedDiagnosticsStatus) {
                    DiagnosticsStatus.SHOULD_PRESENT ->
                        Assert.assertTrue("$diagnostic is not found (but should)", diagnostic in buildResult.output)
                    DiagnosticsStatus.SHOULD_BE_ABSENT ->
                        Assert.assertFalse("$diagnostic is found (but should not)", diagnostic in buildResult.output)
                }
            }
            Assert.assertEquals(expectedOutcome, buildResult.task(":$taskName")?.outcome)
            expectedException?.let {
                Assert.assertTrue("Exception '$it' is not found (but should)", it in buildResult.output)
            }
        }

        private fun assertInspectionBuildXmlReport() {
            if (!xmlReport) return
            fun File.toRootElement() = SAXBuilder().build(this).rootElement

            val actualFile = File(testProjectDir.root, "build/report.xml")
            val expectedFile = File(testDir, "report.xml")
            val actualRoot = actualFile.toRootElement()
            val expectedRoot = expectedFile.toRootElement()
            val xmlOutputter = XMLOutputter(Format.getPrettyFormat())
            val actualRepresentation = xmlOutputter.outputString(actualRoot)
            val expectedRepresentation = xmlOutputter.outputString(expectedRoot)
            Assert.assertEquals(expectedRepresentation, actualRepresentation)
        }

        private fun assertInspectionBuildHtmlReport() {
            if (!htmlReport) return
            val actualFile = File(testProjectDir.root, "build/report.html")
            val expectedFile = File(testDir, "report.html")
            Assert.assertEquals(expectedFile.readText().trim().replace("\r\n", "\n"),
                    actualFile.readText().trim().replace("\r\n", "\n"))
        }

        private fun assertInspectionBuildProjectFiles(actualFiles: List<File>) {
            val actualPathFiles = actualFiles.map { it.relativeTo(testProjectDir.root) }
            val expectedPathFiles = expectedSources.asSequence()
                    .map { it.relativeTo(testDir) }
                    .map {
                        when {
                            it.isKotlinExpectedSource -> File("src/main/kotlin", it.path)
                            it.isJavaExpectedSource -> File("src/main/java", it.path)
                            else -> throw IllegalArgumentException("Undefined language of source file $it")
                        }
                    }
                    .toList()
            @Suppress("UNUSED_VARIABLE")
            for ((expectedPathFile, expectedFile) in expectedPathFiles.zip(expectedSources)) {
                val (actualPathFile, actualFile) = actualPathFiles.zip(actualFiles)
                        .find { it.first.isSourceFor(expectedPathFile) }
                        ?: throw IllegalArgumentException("Actual source for $expectedFile not found")
                val actualCode = actualFile.readText()
                val expectedCode = expectedFile.readText()
                Assert.assertEquals(expectedCode, actualCode)
            }
        }
    }

    override fun doTest(
            arguments: InspectionPluginExtension,
            testDir: File,
            sources: List<File>,
            expectedSources: List<File>,
            xmlReport: Boolean,
            htmlReport: Boolean,
            kotlinVersion: String,
            expectedOutcome: TestBench.TaskOutcome,
            expectedException: String?,
            expectedDiagnosticsStatus: DiagnosticsStatus,
            vararg expectedDiagnostics: String
    ) {
        val expectedGradleOutcome = when (expectedOutcome) {
            BenchTaskOutcome.FAILED -> GradleTaskOutcome.FAILED
            BenchTaskOutcome.SUCCESS -> GradleTaskOutcome.SUCCESS
        }

        InspectionTestConfiguration(
                testDir,
                sources,
                expectedSources,
                arguments,
                xmlReport,
                htmlReport,
                kotlinVersion,
                expectedGradleOutcome,
                expectedException,
                expectedDiagnosticsStatus,
                *expectedDiagnostics
        ).doTest()
    }
}