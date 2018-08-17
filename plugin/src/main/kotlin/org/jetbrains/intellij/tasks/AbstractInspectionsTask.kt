package org.jetbrains.intellij.tasks

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.jetbrains.intellij.Runner
import org.jetbrains.intellij.ChildFirstClassLoader
import org.jetbrains.intellij.ExceptionHandler
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.parameters.InspectionsParameters
import java.io.File
import java.net.URLClassLoader
import java.util.function.BiFunction
import kotlin.concurrent.thread

abstract class AbstractInspectionsTask : SourceTask(), VerificationTask {

    companion object {
        // TODO: take the same version as plugin
        const val runnerVersion = "0.2.0-RC-1"
    }

    @get:Internal
    private var runner: Runner<InspectionsParameters>? = null

    @Internal
    abstract fun getInspectionsParameters(): InspectionsParameters

    abstract fun createRunner(loader: ClassLoader): Runner<InspectionsParameters>

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
            val ideaVersion = parameters.ideaVersion
            val ideaDirectory = InspectionPlugin.ideaDirectory(ideaVersion)
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
            val kotlinPluginVersion = parameters.kotlinPluginVersion
            val kotlinPluginDirectory = InspectionPlugin.kotlinPluginDirectory(kotlinPluginVersion, ideaVersion)
            val plugins = listOf(kotlinPluginDirectory)
            var success = true
            val inspectionsThread = thread(start = false) {
                val runner = createRunner(loader)
                this.runner = runner
                runner.setLogger(BiFunction { level, message ->
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
                success = runner.run(
                        files = getSource().files,
                        projectName = project.rootProject.name,
                        moduleName = project.name,
                        ideaHomeDirectory = ideaDirectory,
                        plugins = plugins,
                        parameters = parameters
                )
            }
            inspectionsThread.contextClassLoader = loader
            inspectionsThread.setUncaughtExceptionHandler { t, e ->
                success = false
                ExceptionHandler.exception(this, e, "Analyzing exception")
            }
            inspectionsThread.start()
            inspectionsThread.join()

            if (!success && !parameters.ignoreFailures) {
                ExceptionHandler.exception(this, "Task execution failure")
            }
        } catch (e: Throwable) {
            ExceptionHandler.exception(this, e, "Process inspection task exception")
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
            runner?.finalize()
            runner = null
        }

        override fun projectsLoaded(gradle: Gradle?) {}

        override fun buildStarted(gradle: Gradle?) {}

        override fun projectsEvaluated(gradle: Gradle?) {}

        override fun settingsEvaluated(settings: Settings?) {}
    }
}
