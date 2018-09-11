import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    extra["kotlinVersion"] = "1.2.0"
    val kotlinVersion: String by extra

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

val kotlinVersion: String by extra

plugins {
    java
}

apply {
    plugin("kotlin")
}

repositories {
    mavenCentral()
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    implementation("com.googlecode.json-simple:json-simple:1.1")
}
