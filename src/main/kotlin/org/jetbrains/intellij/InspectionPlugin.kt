package org.jetbrains.intellij

import org.gradle.api.Project
import org.gradle.api.Plugin

class InspectionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val inspectionExtension = target.extensions.create("inspections", InspectionPluginExtension::class.java)
        with (inspectionExtension) {

        }
        val inspectionTask = target.tasks.create("analyze", InspectionTask::class.java)
        with (inspectionTask) {

        }
    }
}