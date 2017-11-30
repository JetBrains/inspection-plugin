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
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
        mavenLocal()
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:inspection-plugin:0.1")
    }
}

apply {
    plugin("org.jetbrains.intellij.inspections")
}