[![JetBrains incubator project](http://jb.gg/badges/incubator-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/ProjectsWrittenInKotlin_InspectionPlugin.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=ProjectsWrittenInKotlin_InspectionPlugin&branch_Kotlin=%3Cdefault%3E&tab=buildTypeStatusDiv)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# IDEA inspection plugin

This plugin is intended to run IDEA inspections during Gradle build.

Current status: beta version 0.3.2 is available. 

## Examples

* Regular Gradle project -- see [sample](https://github.com/JetBrains/inspection-plugin/tree/master/sample) subdirectory here
* Another regular Gradle project -- [here](https://github.com/Kotlin-Polytech/KotlinAsFirst/tree/gradle) 
* Android project -- see [another sample](https://github.com/mglukhikh/inspection-sample-android) 

## Usage

* Add `maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }` to your buildscript repositories (temporary location)
* Add `classpath 'org.jetbrains.intellij.plugins:inspection-plugin:0.3.2'` to your buildscript dependencies
* Apply plugin `'org.jetbrains.intellij.inspections'` to your gradle module

This adds one inspection plugin task per source root, normally its name is `inspectionsMain` for `main` root and 
`inspectionsTest` for `test` root respectively. Also plugin adds reformat task per source root. 
They have the same usage (`reformatMain`, `refeomatTest` and etc.)

Also you should specify IDEA version to use and (optionally) kotlin plugin version to use, e.g.

```groovy
inspections {
    idea.version "ideaIC:2017.3"
    plugins.kotlin.version "1.2.61" 
}
``` 

In this example inspections will be taken from IDEA CE version 2017.3. 
Plugin works at least with IDEA CE versions 2017.1, 2017.2, 2017.2.x, 2017.3, 2017.3.x, 2018.1, 2018.1.x, 2018.2, 2018.2.x.
IDEA CE 2018.3, 2018.3.x, 2019.1, 2019.1.x are supported in plugins 0.3.0+ only.
If you have multi-platform project, it's recommended to use IDEA CE 2018.2 or later.
Kotlin plugin versions from 1.2.21 to 1.2.71 are supported directly (required version for your IDE is chosen and downloaded auromatically),
otherwise you will have to specify download URL (see below, `plugins.kotlin.location`).
Kotlin plugin versions 1.3.0 to 1.3.41 are supported directly in plugins 0.3.0+ only.

Plugins 0.2.2 and earlier support only so-called local inspections (most Kotlin inspections fall into this category).
Global inspections (e.g. most Android Lint and part of Java inspections) are supported only in 0.3.0+.

There are three ways to specify inspections for code analysis:

### Inherit from IDEA
```groovy
inspections {
    inheritFromIdea = true
}
```
In this case inspection configuration will be read from file `.idea/inspectionProfiles/Project_Default.xml`.
If `profileName` is not given, `Project_Default.xml` will be used by default.

### Manual inspections list
```groovy
inspections {
    error('org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection')
    error('org.jetbrains.kotlin.idea.inspections.UseExpressionBodyInspection')
    warning('org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection')
    warning('org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection')
    info('org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection')
}
```
In this case inspections from manually defined list will be in use.

Plugins 0.3.0+ allows shorter format (class name only without Inspection suffix):
```groovy
inspections {
    error('DataClassPrivateConstructor')
    error('UseExpressionBody')
    warning('RedundantVisibilityModifier')
    warning('AddVarianceModifier')
    info('ClassHasNoToStringMethod')
}
```

or this way (via displayable inspection names):
```groovy
inspections {
    error("Private data class constructor is exposed via the 'copy' method")
    error("Expression body syntax is preferable here")
    warning("Redundant visibility modifier")
    warning("Type parameter can have 'in' or 'out' variance")
    info("Class does not override 'toString()' method")
}
```

### Mixing definition
```groovy
inspections {
    inheritFromIdea = true
    error('org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection')
    error('org.jetbrains.kotlin.idea.inspections.UseExpressionBodyInspection')
    warning('org.jetbrains.kotlin.idea.inspections.RedundantVisibilityModifierInspection')
    warning('org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection')
    info('org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection')
}
```
In this case both inspections from manually defined list and inherited from IDEA will be in use.

To run inspections, execute from terminal: `gradlew inspectionsMain`.
This will download IDEA artifact to gradle cache,
unzip it to cached temporary dir and launch all inspections.
You will see inspection messages in console as well as in report XML / HTML located in `build/reports/inspections`.

### Auto-formatting

Auto formatting of a source code is an experimental feature. To run auto-formatting, execute from terminal: `gradlew reformatMain`. This will reformat all files from the `main` source root, if the current code style is violated for them. The current code style is read from IDEA project configuration.

## JDK configuration

To run inspections correctly, inspection plugin configures JDK in IDEA used. 
To do it, values of different environment variables are read:

* `JAVA_HOME`: can be used to configure any version of JDK
* `JDK_16`: can be used to configure JDK 1.6
* `JDK_17`: can be used to configure JDK 1.7
* `JDK_18`: can be used to configure JDK 1.8

Path to JDK which is used on your project must be available among these variables.

## Additional options

You can specify additional options in `inspections` closure, e.g.:

```groovy
inspections {
    errors.max = 5
    warnings.max = 20
    info.max = 10
    ignoreFailures = true
    quiet = true
    warning('org.jetbrains.java.generate.inspection.ClassHasNoToStringMethodInspection') {
        quickFix = true
    }
    reformat.quickFix = true
    plugins.kotlin.version = '1.2.60'
    plugins.kotlin.location = 'https://plugins.jetbrains.com/plugin/download?rel=true&updateId=48409'
    tempDirInHome = true
}
```

The meaning of the parameters is the following:

* `errors.max`: after exceeding the given number of inspection diagnostics with "error" severity, inspection task stops and fails.
* `warnings.max`: after exceeding the given number of inspection diagnostics with "warning" severity, inspection task stops and fails.
* `info.max`: after exceeding the given number of inspection diagnostics with "info" severity, inspection task stops and fails.
* `ignoreFailures`: inspection task never fails (false by default)
* `quiet`: do not report inspection messages to console, only to XML / HTML files (false by default)
* `quickFix`: apply quick fixes for corresponding inspection (false by default). Quick-fix is applied only if it's unique per warning.
* `reformat.quiet`: do not report reformat inspection messages to console, only to XML / HTML files (false by default)
* `reformat.quickFix`: apply quick fixes for fixed code-style errors (true by default)
* `plugins.kotlin.version`: version of downloading kotlin plugin (by default used bundled to IDEA)
* `plugins.kotlin.location`: URL of downloading kotlin plugin
* `tempDirInHome`: chooses to store plugin data (like unzipped IDEA or Kotlin plugin) either in home directory (true) or in temp directory (false, default).

If you wish to change location of report file, you should specify it in closure for particular task, e.g.

```groovy
inspectionsMain {
    reports {
        xml {
            destination file("reportFileName")
        }
        html {
            destination file("reportFileName")
        }
    }
}
```

## Bugs and Problems

You can report issues on the relevant tab: https://github.com/JetBrains/inspection-plugin/issues

It's quite probable that plugin does not work yet in some environment.
It may result in various exceptions during IDEA configuration process. 
If you found such a case, please execute:

```
gradlew --stop
gradlew --info --stacktrace inspectionsMain > inspections.log
```

and attach `inspections.log` to the issue. 
Also it's very helpful to specify Gradle version, OS and 
IDEA version used in inspection plugin (which is set in `idea.version` parameter).

Known bugs / problems at this moment (version 0.3.2):

* plugin does not work yet with Ultimate IDEA versions, like ideaIU:2017.3
* analysis of Kotlin JS and common modules is only partially supported
* Kotlin JVM module with common library in dependencies (like kotlin-stdlib-common or kotlin-test) is configured correctly only in IDEA 2018.2 or later, e.g. IC:2018.2 
* IDEA versions 2019.2 or higher aren't supported

## Build and Test
Test code is in `testing` module.

Must do tasks before `:testing:test` task:
- `:cli:publishCliPublicationToMavenLocal`
- `:runner:publishRunnerPublicationToMavenLocal`
- `:plugin:build`
