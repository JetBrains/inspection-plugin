package org.jetbrains.intellij.tasks

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.quality.CheckstyleReports
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.*
import org.jetbrains.intellij.*
import org.jetbrains.intellij.extensions.InspectionPluginExtension
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.parameters.InspectionParameters
import org.jetbrains.intellij.parameters.InspectionsParameters
import org.jetbrains.intellij.parameters.InspectionPluginParameters
import org.jetbrains.intellij.parameters.ReportParameters
import java.io.File
import java.net.URLClassLoader
import java.util.function.BiFunction
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractInspectionsTask : SourceTask(), VerificationTask, Reporting<CheckstyleReports> {

    companion object {
        // TODO: take the same version as plugin
        const val runnerVersion = "0.2.0-RC-1-SNAPSHOT"
    }

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
    override fun getIgnoreFailures(): Boolean = ignoreFailures ?: extension.isIgnoreFailures

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
        get() = InspectionPlugin.ideaVersion(extension.idea.version)

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
     * Binary sources will not participate in the analysis..
     * Default value is the <tt>true</tt>.
     */
    @get:Input
    open val skipBinarySources: Boolean
        get() = extension.skipBinarySources ?: true

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
    open val errorsInspections: Map<String, InspectionParameters>
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
    open val warningsInspections: Map<String, InspectionParameters>
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
    open val infosInspections: Map<String, InspectionParameters>
        get() = getInspections(extension.infos)

    /**
     * The maximum number of infos that are tolerated before stopping the build
     * and setting the failure property (the last if ignoreFailures = false only)
     *
     * @return the maximum number of infos allowed
     */
    @get:Input
    @get:Optional
    open val maxInfos: Int?
        get() = extension.infos.max

    @get:Internal
    private val inspections: Map<String, InspectionParameters>
        get() = errorsInspections + warningsInspections + infosInspections

    @Suppress("LeakingThis")
    @get:Internal
    private val reports = IdeaCheckstyleReports(this)

    /**
     * The reports to be generated by this task.
     */
    @Nested
    override fun getReports(): CheckstyleReports = reports

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * inspection {
     *     reports {
     *         html {
     *             destination "build/codenarc.html"
     *         }
     *         xml {
     *             destination "build/report.xml"
     *         }
     *     }
     * }
     * </pre>
     *
     *
     * @param closure The configuration
     * @return The reports container
     */
    override fun reports(
            @DelegatesTo(value = CheckstyleReports::class, strategy = Closure.DELEGATE_FIRST) closure: Closure<*>
    ): CheckstyleReports = reports(ClosureBackedAction(closure))

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * checkstyleTask {
     *     reports {
     *         html {
     *             destination "build/codenarc.html"
     *         }
     *     }
     * }
     * </pre>
     *
     * @param configureAction The configuration
     * @return The reports container
     */
    override fun reports(configureAction: Action<in CheckstyleReports>): CheckstyleReports {
        configureAction.execute(reports)
        return reports
    }

    @get:Internal
    override lateinit var sourceSetType: SourceSetType

    @get:Internal
    private var ignoreFailures: Boolean? = null

    @get:Internal
    protected val extension: InspectionPluginExtension
        get() = project.extensions.findByType(InspectionPluginExtension::class.java)!!

    @get:Internal
    private var runner: Runner<InspectionPluginParameters>? = null

    @Internal
    private fun getInspections(ex: InspectionsExtension): Map<String, InspectionParameters> {
        val inspections = ex.inspections.map {
            val quickFix = it.value.quickFix ?: false
            InspectionParameters(it.key, quickFix)
        }
        return inspections.map { it.name to it }.toMap()
    }

    @Internal
    private fun getInspectionsParameters(): InspectionPluginParameters {
        val projectDir = project.rootProject.projectDir
        val xml: File? = if (reports.xml.isEnabled) reports.xml.destination else null
        val html: File? = if (reports.html.isEnabled) reports.html.destination else null
        val report = ReportParameters(isQuiet, xml, html)
        val errors = InspectionsParameters(errorsInspections, maxErrors)
        val warnings = InspectionsParameters(warningsInspections, maxWarnings)
        val infos = InspectionsParameters(infosInspections, maxInfos)
        return InspectionPluginParameters(
                getIgnoreFailures(),
                ideaVersion,
                kotlinPluginVersion,
                projectDir,
                report,
                skipBinarySources,
                inheritFromIdea,
                profileName,
                errors,
                warnings,
                infos
        )
    }

    private fun createRunner(loader: ClassLoader): Runner<InspectionPluginParameters> {
        val className = "org.jetbrains.idea.inspections.InspectionsRunner"
        @Suppress("UNCHECKED_CAST")
        val analyzerClass = loader.loadClass(className) as Class<Runner<InspectionPluginParameters>>
        val analyzer = analyzerClass.constructors.first().newInstance()
        return analyzerClass.cast(analyzer)
    }

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
            ExceptionHandler.exception(this, e, "Process inspection task exception") {
                runnerFinalize()
            }
        }
    }

    private fun runnerFinalize() {
        runner?.finalize()
        runner = null
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
        override fun buildFinished(result: BuildResult?) = runnerFinalize()

        override fun projectsLoaded(gradle: Gradle?) {}

        override fun buildStarted(gradle: Gradle?) {}

        override fun projectsEvaluated(gradle: Gradle?) {}

        override fun settingsEvaluated(settings: Settings?) {}
    }

    init {
        outputs.upToDateWhen { false }
    }
}
