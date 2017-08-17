package com.intellij.idea

import org.jetbrains.idea.inspections.InspectionRunner

fun createCommandLineApplication(isInternal: Boolean, isUnitTestMode: Boolean, isHeadless: Boolean) =
        CommandLineApplication.ourInstance ?: run {
            InspectionRunner.logger.info("===== Creating command line application ===== ")
            CommandLineApplication(isInternal, isUnitTestMode, isHeadless)
        }