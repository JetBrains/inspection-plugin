package org.jetbrains.intellij

import java.io.File

data class QuickFixParameters(val destination: File?, val hasQuickFix: Boolean)