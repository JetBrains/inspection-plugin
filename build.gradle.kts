allprojects {
    repositories {
        jcenter()
    }
}

buildscript {
    repositories {
        mavenCentral()
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
        mavenLocal()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:inspection-plugin:0.1.1")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.0")
    }
}

apply {
    plugin("org.jetbrains.intellij.inspections")
}