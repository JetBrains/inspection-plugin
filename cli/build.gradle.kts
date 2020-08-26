import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.idea.inspections.*

buildscript {
    extra["kotlinVersion"] = "1.3.72"
    extra["kotlinArgParserVersion"] = "2.0.7"
    val kotlinVersion: String by extra

    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.github.jengelman.gradle.plugins:shadow:6.0.0")
    }
}

val kotlinVersion: String by extra
val kotlinArgParserVersion: String by extra

plugins {
    java
    kotlin("jvm") version "1.3.72"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.jfrog.bintray") version "1.8.4"
}

val projectName = "inspection-cli"

configure<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "org.jetbrains.intellij.InspectionTool"
    }
}

configure<ShadowJar>("shadowJar") {
    baseName = projectName
    classifier = ""
}

configure<PublishingExtension> {
    repositories {
        maven {
            url = uri("build/repository")
        }
    }
    publications {
        create<MavenPublication>("Cli") {
            configure<ShadowExtension> {
                component(this@create)
            }
            version = projectVersion
            groupId = projectGroup
            artifactId = projectName
        }
    }
}

configure<BintrayExtension> {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    pkg = PackageConfig().apply {
        userOrg = "kotlin"
        repo = "kotlin-dev"
        name = "inspections"
        desc = "IDEA inspection offline running tool"
        vcsUrl = "https://github.com/mglukhikh/inspection-plugin.git"
        setLicenses("Apache-2.0")
        version = VersionConfig().apply {
            name = projectVersion
        }
    }

    setPublications("Cli")
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compile("com.xenomachina:kotlin-argparser:$kotlinArgParserVersion")
    implementation("com.googlecode.json-simple:json-simple:1.1")
    compile("org.jdom:jdom2:2.0.6")
    compile(project(":interface"))
    compile(project(":frontend"))
}
