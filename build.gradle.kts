import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    extra["kotlinVersion"] = "1.1.4-eap-69"
    val kotlinVersion: String by extra

	repositories {
		mavenCentral()
        mavenLocal()
        jcenter()
		maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap-1.1") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
	}

	dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0-RC2")
        classpath("gradle.plugin.org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.2.9")
    }
}

val kotlinVersion: String by extra

apply {
	plugin("org.junit.platform.gradle.plugin")
    plugin("org.jetbrains.intellij")
    plugin("java-gradle-plugin")
    plugin("maven-publish")
    plugin("kotlin")
}

val projectGroup = "org.jetbrains.intellij.plugins"
val projectVersion = "0.1-SNAPSHOT"
val projectName = "inspection-plugin"

group = projectGroup
version = projectVersion

configure<IntelliJPluginExtension> {
    version = "IC-2017.2"
    setPlugins("Kotlin")
}

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
        create<MavenPublication>("JCenterPublication") {
            from(components.getByName("java"))
            version = projectVersion
            groupId = projectGroup
            artifactId = projectName
        }
    }
}

repositories {
	mavenCentral()
    mavenLocal()
	maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap-1.1") }
}

tasks {
    val kotlinVersion = "1.1.4-eap-69"

	withType<KotlinCompile> {
		kotlinOptions {
			jvmTarget = "1.8"
		}
	}

	dependencies {
		compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
		compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
        compile(gradleApi())

		testCompile("org.junit.jupiter:junit-jupiter-api:5.0.0-RC2")
		testRuntime("org.junit.jupiter:junit-jupiter-engine:5.0.0-RC2")
		testRuntime("org.junit.platform:junit-platform-launcher:1.0.0-RC2")
	}
}
