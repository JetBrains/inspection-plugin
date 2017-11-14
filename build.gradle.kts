allprojects {

    group = "org.jetbrains.intellij.plugins"

    version = "0.1-SNAPSHOT"

    repositories {
        jcenter()
    }
}

buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:inspection-plugin:0.1-SNAPSHOT")
    }
}

apply {
    plugin("org.jetbrains.intellij.inspections")
}