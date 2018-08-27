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
import org.jetbrains.intellij.parameters.FileInfoRunnerParameters
import org.jetbrains.intellij.parameters.IdeaRunnerParameters
import org.jetbrains.intellij.parameters.InspectionsRunnerParameters
import java.io.File
import java.util.*
import org.jetbrains.intellij.ProxyRunner

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
        return IdeaRunnerParameters(
                projectDir = project.rootProject.projectDir,
                projectName = project.rootProject.name,
                moduleName = project.name,
                ideaVersion = ideaVersion,
                ideaHomeDirectory = ideaDirectory,
                ideaSystemDirectory = ideaSystemDirectory,
                kotlinPluginDirectory = kotlinPluginDirectory,
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

    private val pluginVersion by lazy {
        val propertiesPath = "/META-INF/gradle-plugins/org.jetbrains.intellij.inspections.properties"
        val resources = InspectionPlugin::class.java.getResourceAsStream(propertiesPath)
        val properties = Properties()
        properties.load(resources)
        properties.getProperty("version")
    }

    private fun Project.getDependencyJar(dependencyIdentifier: String): File = try {
        val dependency = project.buildscript.dependencies.create(dependencyIdentifier)
        val configuration = project.buildscript.configurations.detachedConfiguration(dependency)
        configuration.description = "Runner main jar"
        configuration.resolve().first()
    } catch (e: Exception) {
        project.parent?.getDependencyJar(dependencyIdentifier) ?: throw e
    }

    private fun Project.getProjectJar(projectName: String): File {
        val identifier = "org.jetbrains.intellij.plugins:$projectName:$pluginVersion"
        return getDependencyJar(identifier)
    }

    private val File.classpath: List<File>
        get() = listFiles { file, name -> name.endsWith("jar") && "xmlrpc" !in name }?.toList()
                ?: throw IllegalStateException("Files not found in directory $this")

    private fun getIdeaClasspath(ideaDirectory: File): List<File> {
        val ideaLibraries = File(ideaDirectory, "lib")
        return ideaLibraries.classpath
    }

    @Suppress("unused")
    @TaskAction
    fun run() {
        try {
            val parameters = getIdeaRunnerParameters()
            val jar = project.getProjectJar("inspection-runner")
            logger.info("InspectionPlugin: Backend jar: $jar")
            val ideaHomeDirectory = parameters.ideaHomeDirectory
            logger.info("Idea home directory: $ideaHomeDirectory")
            val ideaClasspath = getIdeaClasspath(ideaHomeDirectory)
            logger.info("Idea classpath: $ideaClasspath")
            val runner = Runner.getOrInit {
                project.gradle.root.addBuildListener(IdeaFinishingListener())
                ProxyRunner(jar, ideaClasspath) { level, message ->
                    when (level) {
                        Logger.Level.ERROR -> logger.error("InspectionPlugin: $message")
                        Logger.Level.WARNING -> logger.warn("InspectionPlugin: $message")
                        Logger.Level.INFO -> logger.info("InspectionPlugin: $message")
                    }
                }
            }
            val outcome = runner.run(parameters)
            when (outcome) {
                RunnerOutcome.CRASH -> exception(this, "Caused task execution exception")
                RunnerOutcome.FAIL -> if (!getIgnoreFailures()) exception(this, "Task execution failure")
                RunnerOutcome.SUCCESS -> logger.info("InspectionPlugin: RUN SUCCESS")
            }
        } catch (e: TaskExecutionException) {
            Runner.finalize(logger)
            throw e
        } catch (e: Throwable) {
            logger.error("InspectionPlugin: Exception during running: ${e.message}")
            Runner.finalize(logger)
            throw TaskExecutionException(this, e)
        }
    }

    private val Gradle.root: Gradle
        get() {
            var gradle: Gradle = this
            while (true) {
                gradle = gradle.parent ?: break
            }
            return gradle
        }

    object Runner {
        private var proxyRunner: ProxyRunner? = null

        fun getOrInit(get: () -> ProxyRunner): ProxyRunner {
            val runner = proxyRunner ?: get()
            proxyRunner = runner
            return runner
        }

        fun finalize(logger: org.gradle.api.logging.Logger) {
            logger.info("InspectionPlugin: Finalize")
            proxyRunner?.finalize()
            proxyRunner = null
        }
    }

    inner class IdeaFinishingListener : BuildListener {
        override fun buildFinished(result: BuildResult?) = Runner.finalize(logger)

        override fun projectsLoaded(gradle: Gradle?) {}

        override fun buildStarted(gradle: Gradle?) {}

        override fun projectsEvaluated(gradle: Gradle?) {}

        override fun settingsEvaluated(settings: Settings?) {}
    }

    init {
        outputs.upToDateWhen { false }
    }
}
