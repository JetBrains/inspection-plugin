package org.jetbrains.intellij

import java.io.File

data class Report(val isQuiet: Boolean, val html: File?, val xml: File?)