allprojects {
    repositories {
        jcenter()
    }
}

buildscript {
    repositories {
        mavenCentral()
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }

    // NB: to publish, add classpath of bintray plugin here
    // and remove its versions from children' build.gradle.kts.
    // However, classpath here breaks sample compilation
    // so I do not include it
}
