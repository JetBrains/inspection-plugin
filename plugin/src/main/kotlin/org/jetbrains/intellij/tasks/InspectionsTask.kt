package org.jetbrains.intellij.tasks

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.plugins.quality.CheckstyleReports
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.*
import org.jetbrains.intellij.*
import org.jetbrains.intellij.Analyzer
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.parameters.InspectionTypeParameters
import org.jetbrains.intellij.parameters.InspectionsParameters
import org.jetbrains.intellij.parameters.ReportParameters
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import java.io.File
import org.gradle.api.Project as GradleProject

@Suppress("MemberVisibilityCanBePrivate")
@CacheableTask
open class InspectionsTask : AbstractInspectionsTask(), Reporting<CheckstyleReports> {

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
    override fun getIgnoreFailures(): Boolean = extension.isIgnoreFailures

    /**
     * Whether this task will ignore failures and continue running the build.
     */
    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        extension.isIgnoreFailures = ignoreFailures
    }

    /**
     * Version of IDEA.
     */
    @get:Input
    var ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(extension.ideaVersion)
        set(value) {
            extension.ideaVersion = value.value
        }

    /**
     * Version of IDEA Kotlin Plugin.
     */
    @get:Input
    @get:Optional
    var kotlinPluginVersion: KotlinPluginVersion?
        get() = InspectionPlugin.kotlinPluginVersion(extension.kotlinPluginVersion, extension.kotlinPluginLocation)
        set(value) {
            extension.kotlinPluginVersion = value?.value
        }

    /**
     * Normally false. Value of true is used in tests to prevent IDEA shutdown.
     */
    @get:Input
    var testMode: Boolean
        get() = extension.testMode ?: false
        set(value) {
            extension.testMode = value
        }

    /**
     * Whether rule violations are to be displayed on the console.
     *
     * @return false if violations should be displayed on console, true otherwise
     */
    @get:Console
    var isQuiet: Boolean
        get() = extension.isQuiet ?: false
        set(value) {
            extension.isQuiet = value
        }

    /**
     * Quick fix are to be executed if found fixable errors.
     * Default value is the <tt>false</tt>.
     */
    @get:Input
    var quickFix: Boolean
        get() = extension.quickFix ?: false
        set(value) {
            extension.quickFix = value
        }

    /**
     * Binary sources will not participate in the analysis..
     * Default value is the <tt>true</tt>.
     */
    @get:Input
    var skipBinarySources: Boolean
        get() = extension.skipBinarySources ?: true
        set(value) {
            extension.skipBinarySources = value
        }

    /**
     * If this value is <tt>true</tt> implementation of inspections will be found in IDEA
     * profile with given {@profileName}.
     */
    @get:Input
    var inheritFromIdea: Boolean
        get() = extension.inheritFromIdea ?: inspections.isEmpty()
        set (value) {
            extension.inheritFromIdea = value
        }

    /**
     * Name of profiles in IDEA. Needed for finding implementation of inspections.
     */
    @get:Input
    @get:Optional
    var profileName: String?
        get() = extension.profileName
        set(value) {
            extension.profileName = value
        }

    /**
     * The inspections with error problem level.
     */
    @get:Input
    var errorsInspections: Set<String>
        get() = extension.errors.inspections ?: emptySet()
        set(value) {
            extension.errors.inspections = value
        }

    /**
     * The maximum number of errors that are tolerated before stopping the build
     * and setting the failure property (the last if ignoreFailures = false only)
     *
     * @return the maximum number of errors allowed
     */
    @get:Input
    @get:Optional
    var maxErrors: Int?
        get() = extension.errors.max
        set(value) {
            extension.errors.max = value
        }

    /**
     * The inspections with warning problem level.
     */
    @get:Input
    var warningsInspections: Set<String>
        get() = extension.warnings.inspections ?: emptySet()
        set(value) {
            extension.warnings.inspections = value
        }

    /**
     * The maximum number of warnings that are tolerated before stopping the build
     * and setting the failure property (the last if ignoreFailures = false only)
     *
     * @return the maximum number of warnings allowed
     */
    @get:Input
    @get:Optional
    var maxWarnings: Int?
        get() = extension.warnings.max
        set(value) {
            extension.warnings.max = value
        }

    /**
     * The inspections with information problem level.
     */
    @get:Input
    var infosInspections: Set<String>
        get() = extension.infos.inspections ?: emptySet()
        set(value) {
            extension.infos.inspections = value
        }

    /**
     * The maximum number of infos that are tolerated before stopping the build
     * and setting the failure property (the last if ignoreFailures = false only)
     *
     * @return the maximum number of infos allowed
     */
    @get:Input
    @get:Optional
    var maxInfos: Int?
        get() = extension.infos.max
        set(value) {
            extension.infos.max = value
        }

    private val inspections: Set<String>
        get() = errorsInspections + warningsInspections + infosInspections

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

    override lateinit var baseType: BaseType

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!

    override fun getInspectionsParameters(): InspectionsParameters {
        val projectDir = project.rootProject.projectDir
        val xml: File? = if (reports.xml.isEnabled) reports.xml.destination else null
        val html: File? = if (reports.html.isEnabled) reports.html.destination else null
        val report = ReportParameters(isQuiet, xml, html)
        val errors = InspectionTypeParameters(errorsInspections, maxErrors)
        val warnings = InspectionTypeParameters(warningsInspections, maxWarnings)
        val infos = InspectionTypeParameters(infosInspections, maxInfos)
        return InspectionsParameters(
                ignoreFailures,
                ideaVersion,
                kotlinPluginVersion,
                projectDir,
                report,
                quickFix,
                skipBinarySources,
                inheritFromIdea,
                profileName,
                errors,
                warnings,
                infos
        )
    }

    override fun createAnalyzer(loader: ClassLoader): Analyzer<InspectionsParameters> {
        val className = "org.jetbrains.idea.inspections.InspectionsRunner"
        @Suppress("UNCHECKED_CAST")
        val analyzerClass = loader.loadClass(className) as Class<Analyzer<InspectionsParameters>>
        val analyzer = analyzerClass.constructors.first().newInstance(testMode)
        return analyzerClass.cast(analyzer)
    }

    init {
        outputs.upToDateWhen { false }
    }
}