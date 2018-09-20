import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.idea.inspections.*

buildscript {
    extra["kotlinVersion"] = "1.2.0"
    val kotlinVersion: String by extra

    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.1")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    }
}

val kotlinVersion: String by extra

plugins {
    java
}

apply {
    plugin("kotlin")
    plugin("java-gradle-plugin")
    plugin("maven-publish")
    plugin("com.github.johnrengelman.shadow")
    plugin("com.jfrog.bintray")
}

val projectName = "inspection-plugin"

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
        create<MavenPublication>("Plugin") {
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

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    compileOnly(gradleApi())
    compile("org.apache.httpcomponents:httpclient:4.2.2")
    compile(project(":interface"))
    compile(project(":frontend"))
}

configure<ProcessResources>("processResources") {
    eachFile {
        if (name == "org.jetbrains.intellij.inspections.properties") {
            filter {
                it.replace("<version>", projectVersion)
            }
        }
    }
}
