package org.jetbrains.intellij

import org.gradle.api.Project
import org.gradle.api.Plugin

class InspectionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("inspections", InspectionPluginExtension::class.java)
    }
}