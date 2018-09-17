package org.jetbrains.intellij

import java.io.File

data class Structure(val projectName: String, val modules: List<Module>) {
    data class Module(val name: String, val directory: File, val sourceSets: Set<File>)
}