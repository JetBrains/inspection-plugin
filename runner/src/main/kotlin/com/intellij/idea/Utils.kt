package com.intellij.idea

fun createCommandLineApplication(isInternal: Boolean, isUnitTestMode: Boolean, isHeadless: Boolean) =
        CommandLineApplication(isInternal, isUnitTestMode, isHeadless)

fun getCommandLineApplication(): CommandLineApplication? =
        CommandLineApplication.ourInstance