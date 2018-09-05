package org.jetbrains.intellij.utils

import org.jetbrains.intellij.configurations.MARKERS_DIRECTORY
import java.io.File

class UpToDateChecker(private val identificator: String) {

    private val markerFile by lazy { File(MARKERS_DIRECTORY, "unpack-$identificator.marker") }

    private val isUpToDate by lazy { markerFile.exists() }

    fun onUpToDate(action: () -> Unit) = doIf(isUpToDate, action)

    fun onNonActual(action: () -> Unit) = doIf(!isUpToDate) {
        action()
        markerFile.parentFile.mkdirs()
        markerFile.createNewFile()
    }

    private fun doIf(condition: Boolean, action: () -> Unit) = if (condition) action() else Unit
}