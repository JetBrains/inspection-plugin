[![JetBrains incubator project](http://jb.gg/badges/incubator-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# IDEA inspection plugin

This plugin is intended to run IDEA inspections during Gradle build.

Current status: alpha version 0.1.1 available.

## Usage

* Add `maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }` to your buildscript repositories (temporary location)
* Add `classpath 'org.jetbrains.intellij.plugins:inspection-plugin:0.1.1'` to your buildscript dependencies
* Apply plugin `'org.jetbrains.intellij.inspections'` to your gradle module

This adds one inspection plugin task per source root, 
normally its name is `inspectionsMain` for `main` root
and `inspectionsTest` for `test` root respectively.

Also you should specify IDEA version to use, e.g.

```groovy
inspections {
    toolVersion "ideaIC:2017.2.6"
}
``` 

In this example inspections will be taken from IDEA CE version 2017.2.6. 
Plugin should work with version 2017.2 or later ones.

And the last necessary thing is configuration file, 
which is located (by default) in file `config/inspections/inspections.xml`.
The simplest possible format is the following:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<inspections>
    <inheritFromIdea/>
    <errors/>
    <warnings/>
    <infos/>
</inspections>
```

In this case inspection configuration will be inherited from IDEA.
If IDEA inspection configuration file does not exist in your project,
then default inspection severities will be in use.

To run inspections, execute from terminal: `gradlew inspectionsMain`.
This will download IDEA artifact to gradle cache,
unzip it to cached temporary dir and launch all inspections.
You will see inspection messages in console as well as in report XML located in `build/reports/inspections/main.xml`.

You can find example usage in `sample` project subdirectory.