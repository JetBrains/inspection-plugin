import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.idea.inspections.*

buildscript {
    extra["kotlinVersion"] = "1.3.72"
    val kotlinVersion: String by extra

    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

val kotlinVersion: String by extra

plugins {
    java
    kotlin("jvm") version "1.3.72"
    `java-gradle-plugin`
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

val projectName = "inspection-plugin"

configure<PublishingExtension> {
    repositories {
        maven {
            url = uri("build/repository")
        }
    }
    publications {
        create<MavenPublication>("Plugin") {
            from(components.getByName("java"))
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

    setPublications("Plugin")
}

repositories {
    mavenCentral()
    mavenLocal()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            languageVersion = "1.1"
            apiVersion = "1.1"
        }
    }
}

configurations {
    create("submodules")
    get("compileOnly").extendsFrom(get("submodules"))
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compileOnly(gradleApi())
    compile("org.apache.httpcomponents:httpclient:4.5.5")
    compile("com.googlecode.json-simple:json-simple:1.1")
    add("submodules", project(":interface"))
    add("submodules", project(":frontend"))
}

configure<Jar>("jar") {
    from(configurations["submodules"].map { if (it.isDirectory) it as Any else zipTree(it) })
}

configure<ProcessResources>("processResources") {
    inputs.file("../gradle.properties")
    eachFile {
        if (name == "org.jetbrains.intellij.inspections.properties") {
            filter {
                it.replace("<version>", projectVersion)
            }
        }
    }
}
