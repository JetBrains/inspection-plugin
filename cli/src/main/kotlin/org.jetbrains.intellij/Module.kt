package org.jetbrains.intellij

import java.io.File

data class Module(val name: String, val directory: File, val sourceSets: Set<File>)
