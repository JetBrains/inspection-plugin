package org.jetbrains.intellij.tasks

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.*
import org.jetbrains.intellij.*
import org.jetbrains.intellij.Analyzer
import org.jetbrains.intellij.parameters.InspectionsParameters
import java.io.File
import java.net.URLClassLoader
import java.util.function.BiFunction
import kotlin.concurrent.thread

abstract class AbstractInspectionsTask : SourceTask(), VerificationTask {

    companion object {
        // TODO: take the same version as plugin
        const val runnerVersion = "0.1.5-SNAPSHOT"
    }

    private var analyzer: Analyzer<InspectionsParameters>? = null

    @Internal
    abstract fun getInspectionsParameters(): InspectionsParameters

    abstract fun createAnalyzer(loader: ClassLoader): Analyzer<InspectionsParameters>

    private fun tryResolveRunnerJar(project: org.gradle.api.Project): File = try {
        val runner = "org.jetbrains.intellij.plugins:inspection-runner:$runnerVersion"
        val dependency = project.buildscript.dependencies.create(runner)
        val configuration = project.buildscript.configurations.detachedConfiguration(dependency)
        configuration.description = "Runner main jar"
        configuration.resolve().first()
    } catch (e: Exception) {
        project.parent?.let { tryResolveRunnerJar(it) } ?: throw e
    }

    private val File.classpath: List<File>
        get() = listFiles { dir, name -> name.endsWith("jar") && "xmlrpc" !in name }?.toList()
                ?: throw IllegalStateException("Files not found in directory $this")

    private fun getIdeaClasspath(ideaDirectory: File): List<File> {
        val ideaLibraries = File(ideaDirectory, "lib")
        return ideaLibraries.classpath
    }

    @Suppress("unused")
    @TaskAction
    fun run() {
        try {
            val parameters = getInspectionsParameters()
            val ideaDirectory = InspectionPlugin.ideaDirectory(parameters.ideaVersion, parameters.kotlinPluginVersion)
            logger.info("InspectionPlugin: Idea directory: $ideaDirectory")
            val ideaClasspath = getIdeaClasspath(ideaDirectory)
            val analyzerClasspath = listOf(tryResolveRunnerJar(project))
            val fullClasspath = (analyzerClasspath + ideaClasspath /*+ pluginsClasspath*/).map { it.toURI().toURL() }
            logger.info("InspectionPlugin: Runner classpath: $fullClasspath")
            val parentClassLoader = this.javaClass.classLoader
            logger.info("InspectionPlugin: Runner parent class loader: $parentClassLoader")
            if (parentClassLoader is URLClassLoader) {
                logger.info("InspectionPlugin: Parent classpath: " + parentClassLoader.urLs)
            }
            val loader = ClassloaderContainer.getOrInit {
                ChildFirstClassLoader(fullClasspath.toTypedArray(), parentClassLoader)
            }
            var success = true
            val inspectionsThread = thread(start = false) {
                val analyzer = createAnalyzer(loader)
                this.analyzer = analyzer
                analyzer.setLogger(BiFunction { level, message ->
                    when (level) {
                        0 -> logger.error(message)
                        1 -> logger.warn(message)
                        2 -> logger.info(message)
                    }
                })
                var gradle: Gradle = project.gradle
                while (true) {
                    gradle = gradle.parent ?: break
                }
                gradle.addBuildListener(IdeaFinishingListener())
                success = analyzer.analyze(
                        files = getSource().files,
                        projectName = project.rootProject.name,
                        moduleName = project.name,
                        ideaHomeDirectory = ideaDirectory,
                        parameters = parameters
                )
            }
            inspectionsThread.contextClassLoader = loader
            inspectionsThread.setUncaughtExceptionHandler { t, e ->
                ExceptionHandler.handle(logger, this, e, "InspectionPlugin: Exception during works analyzer")
            }
            inspectionsThread.start()
            inspectionsThread.join()

            if (!success && !parameters.ignoreFailures) {
                val ex = Exception("Task execution failure")
                throw TaskExecutionException(this, ex)
            }
        } catch (e: Throwable) {
            ExceptionHandler.handle(logger, this, e, "InspectionPlugin: Exception caught in inspections plugin")
        }
    }

    object ClassloaderContainer {
        @JvmField
        var customClassLoader: ClassLoader? = null

        fun getOrInit(init: () -> ClassLoader): ClassLoader {
            return customClassLoader ?: init().apply {
                customClassLoader = this
            }
        }
    }

    inner class IdeaFinishingListener : BuildListener {
        override fun buildFinished(result: BuildResult?) {
            analyzer?.finalize()
            analyzer = null
        }

        override fun projectsLoaded(gradle: Gradle?) {}

        override fun buildStarted(gradle: Gradle?) {}

        override fun projectsEvaluated(gradle: Gradle?) {}

        override fun settingsEvaluated(settings: Settings?) {}
    }
}
