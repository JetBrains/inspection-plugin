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
    }
}

val kotlinVersion: String by extra

apply {
    plugin("kotlin")
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
    compile("org.jdom:jdom2:2.0.6")
    compile(fileTree(mapOf("dir" to "$buildDir/idea/lib", "include" to "*.jar")))
    compile(project(":plugin"))
}

