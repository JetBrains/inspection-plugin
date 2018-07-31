package org.jetbrains.intellij.tasks

import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.jetbrains.intellij.Analyzer
import org.jetbrains.intellij.BaseType
import org.jetbrains.intellij.InspectionPlugin
import org.jetbrains.intellij.extensions.InspectionsExtension
import org.jetbrains.intellij.parameters.InspectionTypeParameters
import org.jetbrains.intellij.parameters.InspectionsParameters
import org.jetbrains.intellij.parameters.ReportParameters
import org.jetbrains.intellij.versions.IdeaVersion
import org.jetbrains.intellij.versions.KotlinPluginVersion
import org.jetbrains.intellij.versions.ToolVersion

@Suppress("MemberVisibilityCanBePrivate")
open class ReformatTask : AbstractInspectionsTask() {

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
     * Tool (IDEA) version to use
     */
    @get:Input
    val toolVersion: ToolVersion
        get() = InspectionPlugin.toolVersion(extension.toolVersion)

    /**
     * Version of IDEA.
     */
    @get:Input
    val ideaVersion: IdeaVersion
        get() = InspectionPlugin.ideaVersion(toolVersion, extension.ideaVersion)

    /**
     * Version of IDEA Kotlin Plugin.
     */
    @get:Input
    val kotlinPluginVersion: KotlinPluginVersion
        get() = InspectionPlugin.kotlinPluginVersion(toolVersion, extension.kotlinPluginVersion)

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
    @get:Input
    @get:Console
    var isQuiet: Boolean
        get() = extension.reformat.isQuiet ?: false
        set(value) {
            extension.reformat.isQuiet = value
        }

    /**
     * Auto reformat are to be executed if found fixable code style errors.
     * Default value is the <tt>true</tt>.
     */
    @get:Input
    var quickFix: Boolean
        get() = extension.reformat.quickFix ?: true
        set(value) {
            extension.reformat.quickFix = value
        }

    override lateinit var baseType: BaseType

    private val extension: InspectionsExtension
        get() = project.extensions.findByType(InspectionsExtension::class.java)!!

    override fun getInspectionsParameters(): InspectionsParameters {
        val projectDir = project.rootProject.projectDir
        val report = ReportParameters(isQuiet, null, null)
        val errors = InspectionTypeParameters(setOf(), null)
        val warnings = InspectionTypeParameters(setOf(), null)
        val infos = InspectionTypeParameters(setOf(), null)
        return InspectionsParameters(
                ignoreFailures,
                toolVersion,
                ideaVersion,
                kotlinPluginVersion,
                projectDir,
                report,
                quickFix,
                false,
                null,
                errors,
                warnings,
                infos
        )
    }

    override fun createAnalyzer(loader: ClassLoader): Analyzer<InspectionsParameters> {
        val className = "org.jetbrains.idea.inspections.ReformatInspectionRunner"
        @Suppress("UNCHECKED_CAST")
        val analyzerClass = loader.loadClass(className) as Class<Analyzer<InspectionsParameters>>
        val analyzer = analyzerClass.constructors.first().newInstance(testMode)
        return analyzerClass.cast(analyzer)
    }
}