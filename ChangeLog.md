# CHANGELOG

## 0.2.2

Bug fixes:

 * Use Project_Default.xml inspection profile by default (KT-27528)
 * Add short-cuts for Kotlin plugin 1.2.70, 1.2.71
 * Fix flaky reports of inspections with INFORMATION highlight level (KT-27529)
 
Configuration update:

 * Use `info` instead of `infos`
 
Infrastructure:

 * Handle cache directories more accurately (KT-27637)

## 0.2.1

Bug fixes:

 * Files are now identified by their path in reports, not only their name (KT-27515)
 * Minor infrastructure fixes

## 0.2.0

More versions of Kotlin plugin are supported by default.

## 0.2.0-rc-3

Make gradle configuration a bit nicer

## 0.2.0-rc-2

Bug fixes:

 * KT-25041
 * KT-25042
 * KT-25186
 * gradle build cannot apply reformat / quick fixes

## 0.2.0-RC-1

Additional features:

 * reformat task
 * opportunity to automatically apply quick-fixes
 * opportunity to use given version of kotlin plugin (`plugins.kotlin.version`)

Configuration:

 * main configuration file is no more used at all
 * `toolVersion` -> `idea.version`
 
Bug fixes: 

 * Gradle 4.9 support
 * Tests with different IDEA versions are executed correctly now

## 0.1.4 

Additional features:

 * inspectionsClean task
 
Configuration:

 * main configuration file is now taken from root project directory, not from current module directory

Bug fixes:

 * IDEA 2017.3 & 2018.1 support (most Kotlin inspections did not work in 0.1.3 for these IDEA versions)
 * Various problems with using the plugin on multi-module & multi-platform projects
 * JDK is configured correctly
 * Message "Ultimate is not supported yet" instead of exception when using IU instead of IC
 * Exception inside running inspection stops only this inspection, not the whole inspectionsMain task
 * Inspection tasks now depend on tool version (considered out-of-date when tool version is changed)

## 0.1.3

Bug fixes:

 * IDEA 2017.3 support

## 0.1.2

Additional features:

 * Select and use IDEA inspection profile via `inheritFromIdea profileName="..."` in configuration file
 * Multi-process mode support (two or more plugin tasks can be launched simultaneously)
 * Basic HTML reports
 
Bug fixes:

 * Several fixes in inspection level & log level calculation 

## 0.1.1 (first version)

This alpha-version provides basic functionality:

 * Application of `inspections` plugin
 * Select IDEA version via `toolVersion`
 * Inspection configuration via `config/inspections/inspections.xml`
 * Set of additional options (`maxErrors`, `maxWarnings`, `ignoreFailures`, `quiet`, `config`)
 * Output either to console or XML file
 * Single-process mode (simultaneous work of two or more plugin tasks is not guaranteed)