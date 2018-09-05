package org.jetbrains.intellij.utils

import org.jetbrains.intellij.configurations.markersDirectory
import java.io.File

class UpToDateChecker(private val identifier: String, private val inHome: Boolean) {

    private val markerFile by lazy { File(markersDirectory(inHome), "unpack-$identifier.marker") }

    private val isUpToDate by lazy { markerFile.exists() }

    fun onUpToDate(action: () -> Unit) = doIf(isUpToDate, action)

    fun onNonActual(action: () -> Unit) = doIf(!isUpToDate) {
        action()
        markerFile.parentFile.mkdirs()
        markerFile.createNewFile()
    }

    private fun doIf(condition: Boolean, action: () -> Unit) = if (condition) action() else Unit
}