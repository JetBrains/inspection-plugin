package org.jetbrains.intellij

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

open class InspectionPlugin : Plugin<Project> {
    companion object {
        private val LOG: Logger = Logging.getLogger(InspectionPlugin::class.java)
    }
    override fun apply(target: Project) {
        //target.buildscript.dependencies.add("classpath", target.fileTree(UnzipTask.cacheDirectory.name + "/lib"))
        LOG.debug("1")
        val idea = target.configurations.create("idea")
        val inspectionExtension = target.extensions.create("inspections", InspectionPluginExtension::class.java)
        LOG.debug("2")
        with (inspectionExtension) {
            LOG.debug("ideaVersion = $ideaVersion")
            target.repositories.maven { it.setUrl("https://www.jetbrains.com/intellij-repository/releases") }
            target.dependencies.add(idea.name, "com.jetbrains.intellij.idea:$ideaVersion")
        }
        LOG.debug("3")
        val unzipTask = target.tasks.create("unzip", UnzipTask::class.java)
        with (unzipTask) {

        }
        LOG.debug("4")
        val inspectionTask = target.tasks.create("analyze", InspectionTask::class.java)
        with (inspectionTask) {
            this.setShouldRunAfter(listOf(unzipTask))
        }
        LOG.debug("5")
        inspectionTask.shouldRunAfter(unzipTask)
        LOG.debug("6")
    }
}