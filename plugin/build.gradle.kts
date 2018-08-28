import com.jfrog.bintray.gradle.BintrayExtension
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
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    }
}

val kotlinVersion: String by extra

plugins {
    java
}

apply {
    plugin("java-gradle-plugin")
    plugin("maven-publish")
    plugin("com.jfrog.bintray")
    plugin("kotlin")
}

val projectName = "inspection-plugin"

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create(projectName) {
            id = "org.jetbrains.intellij.inspections"
            implementationClass = "org.jetbrains.intellij.InspectionPlugin"
        }
    }
}

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
            languageVersion = "1.0"
            apiVersion = "1.0"
        }
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    compileOnly(gradleApi())
    // TODO: remove this dependency or make it compile-only
    compile("org.jdom:jdom2:2.0.6")

    testCompile("junit:junit:4.12")
    testCompile(gradleTestKit())
    testCompile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")

    // Version of this library dependent from gradle api version
    compile("org.apache.httpcomponents:httpclient:4.2.2")
    compile(project(":interface"))
}

