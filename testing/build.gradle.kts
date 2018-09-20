import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    extra["kotlinVersion"] = "1.2.0"
    val kotlinVersion: String by extra

    repositories {
        mavenCentral()
        jcenter()
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
    plugin("java-gradle-plugin")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.googlecode.json-simple:json-simple:1.1")
    testCompile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    testCompile("junit:junit:4.12")
    testCompile("org.jdom:jdom2:2.0.6")
    testCompile(gradleTestKit())
    testCompile(project(":plugin"))
    testCompile(project(":cli"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}