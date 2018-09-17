package org.jetbrains.intellij.inspection

import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jetbrains.intellij.*
import org.junit.Assert
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import java.io.*
import java.util.*
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader


class InspectionTestBench(private val defaultTaskName: String) {

    private val testProjectDir = TemporaryFolder(File(System.getProperty("java.io.tmpdir")))

    private val projectDir: File
        get() = testProjectDir.root

    enum class TaskOutcome { FAILED, SUCCESS }

    data class BuildResult(val outcome: TaskOutcome, val output: String)


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

    private enum class DiagnosticsStatus {
        SHOULD_PRESENT,
        SHOULD_BE_ABSENT
    }

    private inner class InspectionTestConfiguration(
            val testDir: File,
            val sources: List<File>,
            val expectedSources: List<File>,
            val toolArguments: ToolArguments,
            val xmlReport: Boolean,
            val htmlReport: Boolean,
            val expectedOutcome: TaskOutcome,
            val expectedException: String?,
            val expectedDiagnosticsStatus: DiagnosticsStatus,
            vararg val expectedDiagnostics: String
    ) {
        fun doTest() {
            testProjectDir.create()
            try {
                testProjectDir.newFolder("build")
                initTestProjectIdeaProfile()
                val testFiles = initTestProjectSources()
                initIdeaProject()
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
            with(toolArguments) {
                val errors = errors ?: emptyList()
                val warnings = warnings ?: emptyList()
                val info = info ?: emptyList()
                if ((errors + warnings + info).isNotEmpty()) {
                    testProjectDir.newFolder(".idea", "inspectionProfiles")
                    val profileName = "Project_Default.xml"
                    val inspectionProfileFile = testProjectDir.newFile(".idea/inspectionProfiles/$profileName")
                    val inspectionProfileContent = generateInspectionProfileFile(errors, warnings, info)
                    writeFile(inspectionProfileFile, inspectionProfileContent)
                    val profilesSettingFile = testProjectDir.newFile(".idea/inspectionProfiles/profiles_settings.xml")
                    writeFile(profilesSettingFile, """
                        <component name="InspectionProjectProfileManager">
                          <settings>
                            <option name="PROJECT_PROFILE" value="Project Default" />
                            <option name="USE_PROJECT_PROFILE" value="true" />
                            <version value="1.0" />
                          </settings>
                        </component>
                        """.trimIndent()
                    )
                }
            }
        }

        private fun initTestProjectSources(): List<File> {
            testProjectDir.newFolder("src", "main", "kotlin")
            testProjectDir.newFolder("src", "main", "java")
            val testFiles = ArrayList<File>()
            for (source in sources) {
                val file = source.relativeTo(testDir)
                val file2 = when {
                    file.isKotlinSource -> File("src/main/kotlin", file.path)
                    file.isJavaSource -> File("src/main/java", file.path)
                    else -> throw IllegalArgumentException("Undefined language of source file $file")
                }
                val dir2 = File(projectDir, file2.parentFile.path)
                if (!dir2.exists()) dir2.mkdirs()
                val file3 = testProjectDir.newFile(file2.path)
                testFiles.add(file3)
            }
            sources.zip(testFiles).forEach { it.first.copyTo(it.second, overwrite = true) }
            return testFiles
        }

        private fun initIdeaProject() {
            val buildFile = testProjectDir.newFile("build.gradle")
            val settingsFile = testProjectDir.newFile("settings.gradle")
            writeFile(buildFile, /*language=groovy*/"""
                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.70"
                    }
                }

                apply plugin: "idea"
                apply plugin: "kotlin"

                sourceSets {
                    main.kotlin.srcDirs += "src"
                    main.java.srcDirs += "src"
                }
            """.trimIndent())
            writeFile(settingsFile, /*language=groovy*/"""
                rootProject.name = '${projectDir.name}'
            """.trimMargin())
            GradleRunner.create().withProjectDir(projectDir).withArguments("idea").build()
        }

        private inner class ComparableByLastModification(val file: File) : Comparable<ComparableByLastModification> {
            override fun compareTo(other: ComparableByLastModification) =
                    file.lastModified().compareTo(other.file.lastModified())
        }

        private fun File.findLatestJar() = listFiles()
                .filter { it.isDirectory }
                .map { ComparableByLastModification(it) }
                .sorted().last().file.listFiles()
                .filter { it.isFile }
                .first { it.extension == "jar" }

        private inner class StreamGobbler private constructor(
                private val input: InputStream,
                private val outputs: List<OutputStream>
        ) : Thread() {
            constructor(input: InputStream, vararg outputs: OutputStream) : this(input, outputs.toList())

            override fun run() {
                try {
                    val input = BufferedReader(InputStreamReader(input))
                    val outputs = outputs.map { BufferedWriter(OutputStreamWriter(it)) }
                    while (true) {
                        val line = input.readLine() ?: break
                        for (output in outputs) {
                            output.write(line)
                            output.write("\n")
                            output.flush()
                        }
                    }
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }
            }
        }

        private fun buildTestProject(): BuildResult {
            val home = System.getProperty("user.home")
            val mavenLocalRepo = File(home, ".m2/repository")
            val workingDirectory = System.getProperty("user.dir")
            val inspectionPluginProjectDirectory = File(workingDirectory, "..").canonicalFile
            val inspectionPluginRepo = File(mavenLocalRepo, "org/jetbrains/intellij/plugins")
            val cli = File(inspectionPluginRepo, "inspection-cli").findLatestJar()
            val runner = File(inspectionPluginRepo, "inspection-runner").findLatestJar()
            val idea = File(inspectionPluginProjectDirectory, "runner/build/ideaIC_2018_2")
            val arguments = toolArguments.copy(
                    idea = toolArguments.idea ?: idea,
                    runner = toolArguments.runner ?: runner,
                    tasks = toolArguments.tasks ?: listOf(defaultTaskName),
                    project = toolArguments.project ?: projectDir,
                    level = toolArguments.level ?: LoggerLevel.INFO
            ).toCommandLineArguments()
            val command = listOf("java", "-jar", cli.absolutePath) + arguments
            val process = ProcessBuilder(command).start()
            println("Process started: ${command.joinToString(" ")}")
            val pipe = ByteArrayOutputStream()
            val outputGobbler = StreamGobbler(process.inputStream, System.out, pipe)
            val errorGobbler = StreamGobbler(process.errorStream, System.err, pipe)
            outputGobbler.start()
            errorGobbler.start()
            process.waitFor()
            outputGobbler.join()
            errorGobbler.join()
            val output = pipe.toString()
            val outcome = when {
                RunnerOutcome.FAIL.toString() in output -> TaskOutcome.FAILED
                RunnerOutcome.CRASH.toString() in output -> TaskOutcome.FAILED
                RunnerOutcome.SUCCESS.toString() in output -> TaskOutcome.SUCCESS
                else -> TaskOutcome.FAILED
            }
            return BuildResult(outcome, output)
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
            Assert.assertEquals(expectedOutcome, buildResult.outcome)
            expectedException?.let {
                Assert.assertTrue("Exception '$it' is not found (but should)", it in buildResult.output)
            }
        }

        private fun assertInspectionBuildXmlReport() {
            if (!xmlReport) return
            fun File.toRootElement() = SAXBuilder().build(this).rootElement

            val actualFile = File(projectDir, "build/report.xml")
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
            val actualFile = File(projectDir, "build/report.html")
            val expectedFile = File(testDir, "report.html")
            Assert.assertEquals(expectedFile.readText().trim().replace("\r\n", "\n"),
                    actualFile.readText().trim().replace("\r\n", "\n"))
        }

        private fun assertInspectionBuildProjectFiles(actualFiles: List<File>) {
            val actualPathFiles = actualFiles.map { it.relativeTo(projectDir) }
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

    fun doTest(testDir: File, toolArguments: ToolArguments) {
        val sources = testDir.allSourceFiles.toList()
        if (sources.isEmpty()) throw IllegalArgumentException("Test directory in $testDir not found.")
        val expectedSources = testDir.allExpectedSourceFiles.toList()
        val lines = sources.map { it.readLines() }.flatten()

        val expectedDiagnostics = sources.map { source ->
            source.readLines().asSequence()
                    .filter { it.startsWith("//") }
                    .map { it.drop(2).trim() }
                    .mapNotNull {
                        when {
                            it.startsWith(':') -> source.name + it
                            it.startsWith("ERROR: :") -> "ERROR: " + source.name + it.removePrefix("ERROR: ")
                            it.startsWith("WARNING: :") -> "WARNING: " + source.name + it.removePrefix("WARNING: ")
                            it.startsWith("INFO: :") -> "INFO: " + source.name + it.removePrefix("INFO: ")
                            else -> null
                        }
                    }
                    .toList()
        }.flatten()

        fun getParameterValue(parameterName: String, defaultValue: String): String {
            val line = lines.singleOrNull { it.startsWith("// $parameterName =") } ?: return defaultValue
            return line.split("=")[1].trim()
        }

        val xmlReport = getParameterValue("xmlReport", "false").toBoolean()
        val htmlReport = getParameterValue("htmlReport", "false").toBoolean()

        val expectedDiagnosticsStatus = lines.asSequence()
                .map { it.trim() }
                .find { it == "// SHOULD_BE_ABSENT" }
                ?.let { DiagnosticsStatus.SHOULD_BE_ABSENT }
                ?: DiagnosticsStatus.SHOULD_PRESENT
        val expectedOutcome = lines.asSequence()
                .map { it.trim() }
                .find { it.startsWith("// FAIL") }
                ?.let { TaskOutcome.FAILED }
                ?: TaskOutcome.SUCCESS
        val expectedException = lines.asSequence()
                .map { it.trim() }
                .find { it.startsWith("// FAIL:") }
                ?.removePrefix("// FAIL:")
                ?.trim()

        InspectionTestConfiguration(
                testDir,
                sources,
                expectedSources,
                toolArguments,
                xmlReport,
                htmlReport,
                expectedOutcome,
                expectedException,
                expectedDiagnosticsStatus,
                *expectedDiagnostics.toTypedArray()
        ).doTest()
    }
}