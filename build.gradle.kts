import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    extra["kotlinVersion"] = "1.1.4"
    val kotlinVersion: String by extra

	repositories {
		mavenCentral()
        mavenLocal()
        jcenter()
        maven { setUrl("https://plugins.gradle.org/m2/") }
	}

	dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0")
    }
}

val kotlinVersion: String by extra

apply {
	plugin("org.junit.platform.gradle.plugin")
    plugin("java-gradle-plugin")
    plugin("maven-publish")
    plugin("kotlin")
}

val projectGroup = "org.jetbrains.intellij.plugins"
val projectVersion = "0.1-SNAPSHOT"
val projectName = "inspection-plugin"

group = projectGroup
version = projectVersion

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
    maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    maven { setUrl("https://www.jetbrains.com/intellij-repository/snapshots") }
}

configurations {
    create("idea")

    dependencies {
        add("idea", create("com.jetbrains.intellij.idea:ideaIC:2017.2@zip"))
    }
}

task<Sync>(name = "unzip") {
    val idea = configurations.getByName("idea")
    dependsOn(idea)

    from(zipTree(idea.singleFile))

    into("$buildDir/idea")
}

tasks {
	withType<KotlinCompile> {
        dependsOn(listOf(tasks.getByName("unzip")))
		kotlinOptions {
			jvmTarget = "1.8"
		}
	}
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    compile(gradleApi())
    compile("jdom:jdom:1.1")
    compile(fileTree(mapOf("dir" to "$buildDir/idea/lib", "include" to "*.jar")))

    testCompile("org.junit.jupiter:junit-jupiter-api:5.0.0")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.0.0")
    testRuntime("org.junit.platform:junit-platform-launcher:1.0.0")
    testCompile(gradleTestKit())
}

