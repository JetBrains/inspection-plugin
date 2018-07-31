package org.jetbrains.intellij.tasks

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.*
import org.jetbrains.intellij.*
import org.jetbrains.intellij.Analyzer
import java.io.File
import java.net.URLClassLoader
import java.util.function.BiFunction
import kotlin.concurrent.thread

abstract class AbstractInspectionsTask : SourceTask(), VerificationTask {

    /**
     * Whether or not this task will ignore failures and continue running the build.
     *
     * @return true if failures should be ignored
     */
    override fun getIgnoreFailures(): Boolean = extension.isIgnoreFailures

    /**
     * Whether this task will ignore failures and continue running the build.
     */
    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        extension.isIgnoreFailures = ignoreFailures
    }

    private var analyzer: Analyzer? = null

    abstract fun getAnalyzerParameters(): AnalyzerParameters

    abstract fun createAnalyzer(loader: ClassLoader): Analyzer

    protected val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!

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

    @Suppress("unused")
    @TaskAction
    fun run() {
        try {
            val ideaDirectory = InspectionPlugin.ideaDirectory(extension.toolVersion)
            logger.info("Idea directory: $ideaDirectory")
            val ideaLibraries = File(ideaDirectory, "lib")
            val ideaClasspath = ideaLibraries.classpath
            val kotlinPluginDirectory = InspectionPlugin.kotlinPluginDirectory(extension.kotlinPluginVersion)
            val kotlinPluginLibraries = File(File(kotlinPluginDirectory, "Kotlin"), "lib")
            val kotlinPluginClasspath = kotlinPluginLibraries.classpath
            val analyzerClasspath = listOf(tryResolveRunnerJar(project))
            val fullClasspath = (analyzerClasspath + ideaClasspath + kotlinPluginClasspath).map { it.toURI().toURL() }
            logger.info("Runner classpath:")
            fullClasspath.forEach { logger.info("    $it") }
            val parentClassLoader = this.javaClass.classLoader
            logger.info("Runner parent class loader: $parentClassLoader")
            if (parentClassLoader is URLClassLoader) {
                logger.info("Parent classpath:")
                parentClassLoader.urLs.forEach { logger.info("    $it") }
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
                        else -> {
                        }
                    }
                })
                var gradle: Gradle = project.gradle
                while (true) {
                    gradle = gradle.parent ?: break
                }
                gradle.addBuildListener(IdeaFinishingListener())
                val parameters = getAnalyzerParameters()
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
                logger.error(e.message)
                throw TaskExecutionException(this, e)
            }
            inspectionsThread.start()
            inspectionsThread.join()

            if (!success && !ignoreFailures) {
                logger.error("Task execution failure")
                throw TaskExecutionException(this, null)
            }
        } catch (e: Throwable) {
            logger.error("EXCEPTION caught in inspections plugin: " + e.message)
            throw TaskExecutionException(this, e)
        }
    }

    companion object {
        // TODO: take the same version as plugin
        const val runnerVersion = "0.1.5-SNAPSHOT"
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
            analyzer?.shutdownIdea()
            analyzer = null
        }

        override fun projectsLoaded(gradle: Gradle?) {}

        override fun buildStarted(gradle: Gradle?) {}

        override fun projectsEvaluated(gradle: Gradle?) {}

        override fun settingsEvaluated(settings: Settings?) {}
    }
}
