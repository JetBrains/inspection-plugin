package org.jetbrains.intellij.tasks

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.intellij.*
import org.jetbrains.intellij.configurations.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.ChildFirstClassLoader
import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File
import java.net.URLClassLoader
import java.util.*
import java.util.function.BiFunction
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractInspectionsTask : SourceTask(), VerificationTask {

    /**
     * The class path containing the compiled classes for the source files to be analyzed.
     */
    @get:Classpath
    lateinit var classpath: FileCollection

    /**
     * {@inheritDoc}
     *
     *
     * The sources for this task are relatively relocatable even though it produces output that
     * includes absolute paths. This is a compromise made to ensure that results can be reused
     * between different builds. The downside is that up-to-date results, or results loaded
     * from cache can show different absolute paths than would be produced if the task was
     * executed.
     */
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree = super.getSource()

    /**
     * Whether or not this task will ignore failures and continue running the build.
     *
     * @return true if failures should be ignored
     */
    @Input
    override fun getIgnoreFailures(): Boolean = ignoreFailures ?: extension.isIgnoreFailures ?: false

    /**
     * Whether this task will ignore failures and continue running the build.
     */
    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        this.ignoreFailures = ignoreFailures
    }

    /**
     * Version of IDEA.
     */
    @get:Input
    open val ideaVersion: String
        get() = ideaVersion(extension.idea.version)

    /**
     * Version of IDEA Kotlin Plugin.
     */
    @get:Input
    @get:Optional
    open val kotlinPluginVersion: String?
        get() = extension.plugins.kotlin.version

    /**
     * Whether rule violations are to be displayed on the console.
     *
     * @return false if violations should be displayed on console, true otherwise
     */
    @get:Console
    open val isQuiet: Boolean
        get() = extension.isQuiet ?: false


    /**
     * Whether code changes can be applied.
     *
     * @return true if code changes can be applied, false otherwise
     */
    @get:Input
    open val isAvailableCodeChanging: Boolean
        get() = true

    /**
     * If this value is <tt>true</tt> implementation of inspections will be found in IDEA
     * profile with given {@profileName}.
     */
    @get:Input
    open val inheritFromIdea: Boolean
        get() = extension.inheritFromIdea ?: inspections.isEmpty()

    /**
     * Name of profiles in IDEA. Needed for finding implementation of inspections.
     */
    @get:Input
    @get:Optional
    open val profileName: String?
        get() = extension.profileName

    /**
     * The inspections with error problem level.
     */
    @get:Input
    open val errorsInspections: Map<String, InspectionsRunnerParameters.Inspection>
        get() = getInspections(extension.errors)

    /**
     * The maximum number of errors that are tolerated before stopping the build
     * and setting the failure property (the last if ignoreFailures = false only)
     *
     * @return the maximum number of errors allowed
     */
    @get:Input
    @get:Optional
    open val maxErrors: Int?
        get() = extension.errors.max

    /**
     * The inspections with warning problem level.
     */
    @get:Input
    open val warningsInspections: Map<String, InspectionsRunnerParameters.Inspection>
        get() = getInspections(extension.warnings)

    /**
     * The maximum number of warnings that are tolerated before stopping the build
     * and setting the failure property (the last if ignoreFailures = false only)
     *
     * @return the maximum number of warnings allowed
     */
    @get:Input
    @get:Optional
    open val maxWarnings: Int?
        get() = extension.warnings.max

    /**
     * The inspections with information problem level.
     */
    @get:Input
    open val infoInspections: Map<String, InspectionsRunnerParameters.Inspection>
        get() = getInspections(extension.info)

    /**
     * The maximum number of information inspections that are tolerated before stopping the build
     * and setting the failure property (the last if ignoreFailures = false only)
     *
     * @return the maximum number of information inspections allowed
     */
    @get:Input
    @get:Optional
    open val maxInfo: Int?
        get() = extension.info.max

    @get:Internal
    private val inspections: Map<String, InspectionsRunnerParameters.Inspection>
        get() = errorsInspections + warningsInspections + infoInspections

    @Internal
    private var ignoreFailures: Boolean? = null

    @get:Internal
    protected val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!

    @Internal
    private var runner: Runner<IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>>? = null

    @Internal
    private fun getInspections(ex: InspectionsExtension): Map<String, InspectionsRunnerParameters.Inspection> {
        val inspections = ex.inspections.map {
            val quickFix = it.value.quickFix ?: false
            InspectionsRunnerParameters.Inspection(it.key, quickFix)
        }
        return inspections.map { it.name to it }.toMap()
    }

    @get:OutputFile
    @get:Optional
    open val xml: File? = null

    @get:OutputFile
    @get:Optional
    open val html: File? = null

    @Internal
    private fun getIdeaRunnerParameters(): IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>> {
        val ideaDirectory = ideaDirectory(ideaVersion)
        val ideaSystemDirectory = IDEA_SYSTEM_DIRECTORY
        val kotlinPluginDirectory = extension.plugins.kotlin.kotlinPluginDirectory(ideaVersion)
        val plugins = listOf(kotlinPluginDirectory)
        return IdeaRunnerParameters(
                projectDir = project.rootProject.projectDir,
                projectName = project.rootProject.name,
                moduleName = project.name,
                ideaHomeDirectory = ideaDirectory,
                ideaSystemDirectory = ideaSystemDirectory,
                plugins = plugins,
                childParameters = getFileInfoParameters()
        )
    }

    @Internal
    private fun getFileInfoParameters(): FileInfoRunnerParameters<InspectionsRunnerParameters> {
        return FileInfoRunnerParameters(
                files = getSource().files.toList(),
                childParameters = getInspectionsRunnerParameters()
        )
    }

    @Internal
    private fun getInspectionsRunnerParameters(): InspectionsRunnerParameters {
        val reportParameters = InspectionsRunnerParameters.Report(isQuiet, xml, html)
        val errors = InspectionsRunnerParameters.Inspections(errorsInspections, maxErrors)
        val warnings = InspectionsRunnerParameters.Inspections(warningsInspections, maxWarnings)
        val info = InspectionsRunnerParameters.Inspections(infoInspections, maxInfo)
        return InspectionsRunnerParameters(
                ideaVersion = ideaVersion,
                kotlinPluginVersion = kotlinPluginVersion,
                isAvailableCodeChanging = isAvailableCodeChanging,
                reportParameters = reportParameters,
                inheritFromIdea = inheritFromIdea,
                profileName = profileName,
                errors = errors,
                warnings = warnings,
                info = info
        )
    }

    private fun createRunner(loader: ClassLoader): Runner<IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>> {
        val className = "org.jetbrains.idea.inspections.InspectionsRunner"
        @Suppress("UNCHECKED_CAST")
        val analyzerClass = loader.loadClass(className) as Class<Runner<IdeaRunnerParameters<FileInfoRunnerParameters<InspectionsRunnerParameters>>>>
        val analyzer = analyzerClass.constructors.first().newInstance()
        return analyzerClass.cast(analyzer)
    }

    private fun getPluginVersion(): String {
        val propertiesPath = "/META-INF/gradle-plugins/org.jetbrains.intellij.inspections.properties"
        val resources = InspectionPlugin::class.java.getResourceAsStream(propertiesPath)
        val properties = Properties()
        properties.load(resources)
        return properties.getProperty("version")
    }

    private fun Project.tryResolveDependencyJar(dependencyIdentifier: String): File = try {
        val dependency = project.buildscript.dependencies.create(dependencyIdentifier)
        val configuration = project.buildscript.configurations.detachedConfiguration(dependency)
        configuration.description = "Runner main jar"
        configuration.resolve().first()
    } catch (e: Exception) {
        project.parent?.tryResolveDependencyJar(dependencyIdentifier) ?: throw e
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
            val ideaHomeDirectory = ideaDirectory(ideaVersion)
            logger.info("InspectionPlugin: Idea directory: $ideaHomeDirectory")
            val ideaClasspath = getIdeaClasspath(ideaHomeDirectory)
            val pluginVersion = getPluginVersion()
            val inspectionsIdentifier = "org.jetbrains.intellij.plugins:inspection-runner:$pluginVersion"
            val runnerIdentifier = "org.jetbrains.intellij.plugins:inspection-runner:$pluginVersion"
            val inspectionsJar = project.tryResolveDependencyJar(inspectionsIdentifier)
            val runnerJar = project.tryResolveDependencyJar(runnerIdentifier)
            logger.info("InspectionPlugin: Runner jar: $runnerJar")
            logger.info("InspectionPlugin: Inspection jar: $inspectionsJar")
            val fullClasspath = (ideaClasspath + runnerJar + inspectionsJar).map { it.toURI().toURL() }
            logger.info("InspectionPlugin: Runner classpath: $fullClasspath")
            val parentClassLoader = this.javaClass.classLoader
            logger.info("InspectionPlugin: Runner parent class loader: $parentClassLoader")
            if (parentClassLoader is URLClassLoader) {
                logger.info("InspectionPlugin: Parent classpath: " + Arrays.toString(parentClassLoader.urLs))
            }
            val loader = ClassloaderContainer.getOrInit {
                ChildFirstClassLoader(fullClasspath.toTypedArray(), parentClassLoader)
            }
            var taskOutcome = Outcome.SUCCESS
            var runOutcome = Outcome.SUCCESS
            val parameters = getIdeaRunnerParameters()
            val inspectionsThread = thread(start = false) {
                val runner = createRunner(loader)
                this.runner = runner
                var gradle: Gradle = project.gradle
                while (true) {
                    gradle = gradle.parent ?: break
                }
                gradle.addBuildListener(IdeaFinishingListener())
                taskOutcome = Outcome(runner.run(parameters))
            }
            inspectionsThread.contextClassLoader = loader
            inspectionsThread.setUncaughtExceptionHandler { t, e ->
                runOutcome = Outcome.FAILURE
                exception(this, e, "Analyzing exception")
            }
            inspectionsThread.start()
            inspectionsThread.join()

            if (runOutcome == Outcome.FAILURE) {
                exception(this, "Caused task execution exception")
            }
            if (taskOutcome == Outcome.FAILURE && !getIgnoreFailures()) {
                exception(this, "Task execution failure")
            }
        } catch (e: TaskExecutionException) {
            runnerFinalize()
            throw e
        } catch (e: Throwable) {
            exception(this, e, "Process inspection task exception") {
                runnerFinalize()
            }
        }
    }

    private fun runnerFinalize() {
        logger.info("InspectionPlugin: Finalize")
        val runner = runner
        this.runner = null
        runner?.finalize()
        ClassloaderContainer.clean()
    }

    object ClassloaderContainer {
        @JvmField
        var customClassLoader: ClassLoader? = null

        fun getOrInit(init: () -> ClassLoader): ClassLoader {
            return customClassLoader ?: init().apply {
                customClassLoader = this
            }
        }

        fun clean() {
            customClassLoader = null
            System.gc()
        }
    }

    inner class IdeaFinishingListener : BuildListener {
        override fun buildFinished(result: BuildResult?) = runnerFinalize()

        override fun projectsLoaded(gradle: Gradle?) {}

        override fun buildStarted(gradle: Gradle?) {}

        override fun projectsEvaluated(gradle: Gradle?) {}

        override fun settingsEvaluated(settings: Settings?) {}
    }

    enum class Outcome {
        SUCCESS, FAILURE;

        companion object {
            operator fun invoke(outcome: Boolean) = when (outcome) {
                true -> SUCCESS
                false -> FAILURE
            }
        }
    }

    init {
        outputs.upToDateWhen { false }
    }
}
