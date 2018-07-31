import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.internal.ConventionTask
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
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.0")
    }
}

val kotlinVersion: String by extra

apply {
    plugin("kotlin")
    plugin("maven-publish")
    plugin("com.github.johnrengelman.shadow")
    plugin("com.jfrog.bintray")
//    plugin("de.undercouch.download")
}

val projectName = "inspection-runner"

val jar: Jar by tasks
jar.apply {
    manifest {
        attributes(mapOf("Main-Class" to "org.jetbrains.idea.inspections.InspectionRunner"))
    }
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    baseName = projectName
    classifier = ""
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

    setPublications("RunnerJar")
}

configure<PublishingExtension> {
    repositories {
        maven {
            url = uri("build/repository")
        }
    }
    publications {
        create<MavenPublication>("RunnerJar") {
            configure<ShadowExtension> {
                component(this@create)
            }
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
    create("kotlin-plugin")

    dependencies {
        add("idea", create("com.jetbrains.intellij.idea:ideaIC:2017.3@zip"))
    }
}

task<Sync>(name = "unzip-idea") {
    with(configurations.getByName("idea")) {
        dependsOn(this)
        from(zipTree(singleFile))
        into("$buildDir/idea")
    }
}

val kotlinPluginLocation = "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=47481"
val kotlinPluginArchive = File(buildDir, "kotlin-plugin.zip")
val kotlinPluginDirectory = File(buildDir, "kotlin-plugin")

task<DefaultTask>(name = "download-kotlin-plugin") {
    val client = org.apache.http.impl.client.DefaultHttpClient()
    val request = org.apache.http.client.methods.HttpGet(kotlinPluginLocation)
    request.addHeader("User-Agent", "User-Agent")
    val response = client.execute(request)
    kotlinPluginArchive.parentFile.mkdirs()
    response.entity.content.use { input ->
        kotlinPluginArchive.outputStream().use { fileOut ->
            input.copyTo(fileOut)
        }
    }
}

task<Sync>(name = "unzip-kotlin-plugin") {
    dependsOn(tasks.getByName("download-kotlin-plugin"))
    from(zipTree(kotlinPluginArchive))
    into(kotlinPluginDirectory)
}

tasks {
    withType<KotlinCompile> {
        dependsOn(tasks.getByName("unzip-idea"))
        dependsOn(tasks.getByName("unzip-kotlin-plugin"))
        kotlinOptions {
            jvmTarget = "1.8"
            languageVersion = "1.0"
            apiVersion = "1.0"
        }
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    compileOnly("org.jdom:jdom2:2.0.6")
    compileOnly(fileTree(mapOf("dir" to "$buildDir/idea/lib", "include" to "*.jar")))
    compileOnly(files(kotlinPluginDirectory/"Kotlin"/"lib"/"kotlin-plugin.jar"))
    compileOnly(project(":plugin"))
}
