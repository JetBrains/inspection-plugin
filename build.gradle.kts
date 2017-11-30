allprojects {

    group = "org.jetbrains.intellij.plugins"

    version = "0.1"

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
        classpath("org.jetbrains.intellij.plugins:inspection-plugin:0.1")
    }
}

apply {
    plugin("org.jetbrains.intellij.inspections")
}