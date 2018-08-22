package org.jetbrains.intellij.inspection

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.jetbrains.intellij.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.junit.Assert
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.*

class InspectionTestBench(private val testProjectDir: TemporaryFolder, private val taskName: String) {

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
                            appendln("    inheritFromIdea = $it")
                        }
                        "profileName" -> profileName?.gradleCode?.let {
                            appendln("    profileName = $it")
                        }
                        "errors.inspections" -> errors.inspections?.forEach { entry ->
                            val name = entry.key.gradleCode
                            if (inheritFromIdea != true) appendln("    errors.inspection($name)")
                            entry.value.quickFix?.gradleCode?.let {
                                appendln("    errors.inspection($name).quickFix = $it")
                            }
                        }
                        "warnings.inspections" -> warnings.inspections?.forEach { entry ->
                            val name = entry.key.gradleCode
                            if (inheritFromIdea != true) appendln("    warnings.inspection($name)")
                            entry.value.quickFix?.gradleCode?.let {
                                appendln("    warnings.inspection($name).quickFix = $it")
                            }
                        }
                        "infos.inspections" -> infos.inspections?.forEach { entry ->
                            val name = entry.key.gradleCode
                            if (inheritFromIdea != true) appendln("    infos.inspection($name)")
                            entry.value.quickFix?.gradleCode?.let {
                                appendln("    infos.inspection($name).quickFix = $it")
                            }
                        }
                        "errors.max" -> errors.max?.gradleCode?.let {
                            appendln("    errors.max = $it")
                        }
                        "warnings.max" -> warnings.max?.gradleCode?.let {
                            appendln("    warnings.max = $it")
                        }
                        "infos.max" -> infos.max?.gradleCode?.let {
                            appendln("    infos.max = $it")
                        }
                        "quiet" -> isQuiet?.gradleCode?.let {
                            appendln("    quiet = $it")
                        }
                        "reformat.quickFix" -> reformat.quickFix?.gradleCode?.let {
                            appendln("    reformat.quickFix = $it")
                        }
                        "ignoreFailures" -> if (isIgnoreFailures) {
                            appendln("    ignoreFailures = ${isIgnoreFailures.gradleCode}")
                        }
                        "idea.version" -> idea.version?.gradleCode?.let {
                            appendln("    idea.version = $it")
                        }
                        "plugins.kotlin.version" -> plugins.kotlin.version?.gradleCode?.let {
                            appendln("    plugins.kotlin.version = $it")
                        }
                        "plugins.kotlin.location" -> plugins.kotlin.location?.gradleCode?.let {
                            appendln("    plugins.kotlin.location = $it")
                        }
                        "xmlDestination" -> if (xmlReport) {
                            appendln("            destination \"build/report.xml\"")
                        }
                        "htmlDestination" -> if (htmlReport) {
                            appendln("            destination \"build/report.html\"")
                        }
                        "kotlin-stdlib" -> if (kotlinNeeded) {
                            appendln("    compile \"org.jetbrains.kotlin:kotlin-stdlib\"")
                        }
                        "kotlin-runtime" -> if (kotlinNeeded) {
                            appendln("    compile \"org.jetbrains.kotlin:kotlin-runtime\"")
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
                val shortClass = inspectionClass.substringAfterLast(".").substringBefore("InspectionsTask")
                appendln("""        <inspection_tool class="$shortClass" enabled="true" level="$level" enabled_by_default="true"/>""")
            }
        }.toString()

    }

    private fun generateInspectionProfileFile(errors: List<String>, warnings: List<String>, infos: List<String>): String {
        return StringBuilder().apply {
            appendln("<component name=\"InspectionProjectProfileManager\">")
            appendln("    <profile version=\"1.0\">\n")
            appendln("        <option name=\"myName\" value=\"Project Default\" /> \n")
            appendln(generateInspectionToolTags("ERROR", errors))
            appendln(generateInspectionToolTags("WARNING", warnings))
            appendln(generateInspectionToolTags("INFO", infos))
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
            val extension: InspectionPluginExtension,
            val xmlReport: Boolean = false,
            val htmlReport: Boolean = false,
            val kotlinVersion: String = "1.2.0",
            val expectedOutcome: TaskOutcome = TaskOutcome.SUCCESS,
            val expectedDiagnosticsStatus: DiagnosticsStatus = DiagnosticsStatus.SHOULD_PRESENT,
            vararg val expectedDiagnostics: String
    ) {
        fun doTest() {
            testProjectDir.newFolder("build")
            initTestProjectIdeaProfile()
            initTestProjectBuildFile()
            val testFiles = initTestProjectSources()
            val buildResult = buildTestProject()
            try {
                assertInspectionBuildLog(buildResult)
                assertInspectionBuildXmlReport()
                assertInspectionBuildHtmlReport()
                assertInspectionBuildProjectFiles(testFiles)
            } finally {
                val daemonPid = ejectDaemonPid(buildResult)
                println("InspectionTestBench: Daemon PID is $daemonPid")
                if (daemonPid != null) waitIdeaRelease(daemonPid)
            }
        }

        private fun initTestProjectIdeaProfile() {
            with(extension) {
                val errors = errors.inspections.keys.toList()
                val warnings = warnings.inspections.keys.toList()
                val infos = infos.inspections.keys.toList()
                if (inheritFromIdea == true && (errors + warnings + infos).isNotEmpty()) {
                    if (profileName != null)
                        throw IllegalArgumentException("Idea profile in inspection test has auto generating")
                    testProjectDir.newFolder(".idea", "inspectionProfiles")
                    profileName = "Project_Default.xml"
                    val inspectionProfileFile = testProjectDir.newFile(".idea/inspectionProfiles/$profileName")
                    val inspectionProfileContent = generateInspectionProfileFile(errors, warnings, infos)
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
                val file2 = when {
                    file.isKotlinSource -> File("src/main/kotlin", file.path)
                    file.isJavaSource -> File("src/main/java", file.path)
                    else -> throw IllegalArgumentException("Undefined language of source file $file")
                }
                val dir2 = File(testProjectDir.root, file2.parentFile.path)
                if (!dir2.exists()) dir2.mkdirs()
                val file3 = testProjectDir.newFile(file2.path)
                testFiles.add(file3)
            }
            sources.zip(testFiles).forEach { it.first.copyTo(it.second, overwrite = true) }
            return testFiles
        }

        private fun buildTestProject(): BuildResult {
            return try {
                GradleRunner.create()
                        .withProjectDir(testProjectDir.root)
                        .withArguments("--no-daemon")
                        .withArguments("--info", "--stacktrace", taskName)
//                        .withDebug(true)
                        // This applies classpath from pluginUnderTestMetadata
                        .withPluginClasspath()
                        // NB: this is necessary to apply actual plugin
                        .apply { withPluginClasspath(pluginClasspath) }
                        .forwardOutput()
                        .build()
            } catch (failure: UnexpectedBuildFailure) {
                println("InspectionTestBench: Exception caught in test.")
                failure.buildResult
            }
        }

        private fun assertInspectionBuildLog(result: BuildResult) {
            for (diagnostic in expectedDiagnostics) {
                when (expectedDiagnosticsStatus) {
                    DiagnosticsStatus.SHOULD_PRESENT ->
                        Assert.assertTrue("$diagnostic is not found (but should)", diagnostic in result.output)
                    DiagnosticsStatus.SHOULD_BE_ABSENT ->
                        Assert.assertFalse("$diagnostic is found (but should not)", diagnostic in result.output)
                }
            }
            Assert.assertEquals(expectedOutcome, result.task(":$taskName")?.outcome)
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
            val expectedPathFiles = expectedSources
                    .map { it.relativeTo(testDir) }
                    .map {
                        when {
                            it.isKotlinExpectedSource -> File("src/main/kotlin", it.path)
                            it.isJavaExpectedSource -> File("src/main/java", it.path)
                            else -> throw IllegalArgumentException("Undefined language of source file $it")
                        }
                    }
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

        private fun String.removePrefix(prefix: String) = if (startsWith(prefix)) substring(prefix.length) else null

        private fun ejectDaemonPid(buildResult: BuildResult) = buildResult.output
                ?.split('\n')
                ?.asSequence()
                ?.map { it.replace("\r", "") }
                ?.mapNotNull { it.removePrefix("InspectionPlugin: Daemon PID is ") }
                ?.firstOrNull()

        private fun waitIdeaRelease(daemonPid: String) {
            val startTime = System.currentTimeMillis()
            println("InspectionTestBench: Start waiting of idea finalize")
            val ideaLockDir = File(System.getProperty("java.io.tmpdir"), "inspection-plugin/locks")
            val lockFile = ideaLockDir.listFiles()?.find { it.name == "$daemonPid.idea-lock" } ?: run {
                println("InspectionTestBench: lock file not found")
                return
            }
            val ideaLockChannel = FileChannel.open(lockFile.toPath(), StandardOpenOption.WRITE) ?: return
            while (ideaLockChannel.tryLock() == null) Thread.yield()
            ideaLockChannel.close()
            val endTime = System.currentTimeMillis()
            val delay = (endTime - startTime).toDouble() / 1000.0
            println("InspectionTestBench: End waiting of idea finalize. Took $delay secs")
        }
    }

    fun doTest(testDir: File, extension: InspectionPluginExtension) {
        val sources = testDir.allSourceFiles.toList()
        if (sources.isEmpty()) throw IllegalArgumentException("Test directory in $testDir not found.")
        val expectedSources = testDir.allExpectedSourceFiles.toList()
        val lines = sources.map { it.readLines() }.flatten()

        val expectedDiagnostics = sources.map { source ->
            source.readLines()
                    .filter { it.startsWith("// :") || it.startsWith("///") }
                    .map { it.drop(3) }
                    .map { if (it.startsWith(':')) source.name + it else it }
        }.flatten()

        fun getParameterValue(parameterName: String, defaultValue: String): String {
            val line = lines.singleOrNull { it.startsWith("// $parameterName =") } ?: return defaultValue
            return line.split("=")[1].trim()
        }

        val xmlReport = getParameterValue("xmlReport", "false").toBoolean()
        val htmlReport = getParameterValue("htmlReport", "false").toBoolean()
        val kotlinVersion = getParameterValue("kotlinVersion", "1.2.0")

        val expectedDiagnosticsStatus = if (lines.contains("// SHOULD_BE_ABSENT")) DiagnosticsStatus.SHOULD_BE_ABSENT else DiagnosticsStatus.SHOULD_PRESENT
        val expectedOutcome = if (lines.contains("// FAIL")) TaskOutcome.FAILED else TaskOutcome.SUCCESS

        InspectionTestConfiguration(
                testDir,
                sources,
                expectedSources,
                extension,
                xmlReport,
                htmlReport,
                kotlinVersion,
                expectedOutcome,
                expectedDiagnosticsStatus,
                *expectedDiagnostics.toTypedArray()
        ).doTest()
    }
}