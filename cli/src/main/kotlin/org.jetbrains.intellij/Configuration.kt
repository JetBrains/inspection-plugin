package org.jetbrains.intellij

import java.io.File

data class Configuration(
        val runner: File,
        val idea: File,
        val kotlin: File,
        val projectName: String,
        val modules: List<Module>,
        val report: Report
)