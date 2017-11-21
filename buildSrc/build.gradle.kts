buildscript {
    extra["kotlinVersion"] = "1.1.4"
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

apply {
    plugin("kotlin")
}

repositories {
    mavenCentral()
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
}
