package com.intellij.idea

fun createCommandLineApplication(isInternal: Boolean, isUnitTestMode: Boolean, isHeadless: Boolean) =
        CommandLineApplication.ourInstance ?: run {
            CommandLineApplication(isInternal, isUnitTestMode, isHeadless)
        }