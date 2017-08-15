package org.jetbrains.intellij

import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.jetbrains.idea.inspections.InspectionRunner
import org.gradle.api.Project as GradleProject

open class InspectionTask : SourceTask() {

    private val sourceFileTree: FileTree by lazy {
        @Suppress("UNCHECKED_CAST")
        val sourceSets = project.properties["sourceSets"] as Iterable<SourceSet>
        logger.info("Property sourceSets = " + sourceSets)
        val sourceFileTree = sourceSets.fold<SourceSet, FileTree?>(null) { prevFileTree, sourceSet ->
            val currentFileTree = sourceSet.allSource.asFileTree
            prevFileTree?.plus(currentFileTree) ?: currentFileTree

        }!!
        logger.info("Analyze source files: " + sourceFileTree.map { it.name })
        sourceFileTree
    }

    override fun getSource(): FileTree = sourceFileTree

    @TaskAction
    fun analyze() {
        try {
            val extension = project.extensions.findByType(InspectionPluginExtension::class.java)
            val inspectionClasses = extension.inspectionClasses

            val runner = InspectionRunner(*inspectionClasses)
            runner.analyzeTreeAndLogResults(getSource(), extension, logger)
        }
        catch (e: Throwable) {
            logger.error(e.message)
            throw TaskExecutionException(this, Exception("Exception occurred in analyze task", e))
        }
    }
}