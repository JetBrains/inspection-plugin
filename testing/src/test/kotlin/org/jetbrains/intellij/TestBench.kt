package org.jetbrains.intellij

import java.io.File

abstract class TestBench<T> {

    enum class TaskOutcome { FAILED, SUCCESS }

    enum class DiagnosticsStatus { SHOULD_PRESENT, SHOULD_BE_ABSENT }

    abstract fun doTest(
            arguments: T,
            testDir: File,
            sources: List<File>,
            expectedSources: List<File>,
            xmlReport: Boolean,
            htmlReport: Boolean,
            kotlinVersion: String,
            expectedOutcome: TaskOutcome,
            expectedException: String?,
            expectedDiagnosticsStatus: DiagnosticsStatus,
            vararg expectedDiagnostics: String
    )

    protected fun List<String>.getParameterValue(parameterName: String, defaultValue: String): String {
        val line = singleOrNull { it.startsWith("// $parameterName =") } ?: return defaultValue
        return line.split("=")[1].trim()
    }

    fun doTest(testDir: File, arguments: T) {
        val sources = testDir.allSourceFiles.toList()
        if (sources.isEmpty()) throw IllegalArgumentException("Test directory in $testDir not found.")
        val expectedSources = testDir.allExpectedSourceFiles.toList()
        val lines = sources.map { it.readLines() }.flatten()
        val expectedDiagnostics = sources.map { source ->
            source.readLines().asSequence()
                    .filter { it.startsWith("//") }
                    .map { it.drop(2).trim() }
                    .mapNotNull {
                        val relativeFileName = source.relativeTo(testDir).toMavenLayout().path
                        when {
                            it.startsWith(':') -> "WARNING: " + relativeFileName + it
                            it.startsWith("ERROR: :") -> "ERROR: " + relativeFileName + it.removePrefix("ERROR: ")
                            it.startsWith("WARNING: :") -> "WARNING: " + relativeFileName + it.removePrefix("WARNING: ")
                            it.startsWith("INFO: :") -> "INFO: " + relativeFileName + it.removePrefix("INFO: ")
                            it.startsWith("ERROR: ") -> it
                            it.startsWith("WARNING: ") -> it
                            it.startsWith("INFO: ") -> it
                            else -> null
                        }
                    }
                    .toList()
        }.flatten()
        val xmlReport = lines.getParameterValue("xmlReport", "false").toBoolean()
        val htmlReport = lines.getParameterValue("htmlReport", "false").toBoolean()
        val kotlinVersion = lines.getParameterValue("kotlinVersion", "1.2.0")
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
        doTest(
                arguments,
                testDir,
                sources,
                expectedSources,
                xmlReport,
                htmlReport,
                kotlinVersion,
                expectedOutcome,
                expectedException,
                expectedDiagnosticsStatus,
                *expectedDiagnostics.toTypedArray()
        )
    }
}