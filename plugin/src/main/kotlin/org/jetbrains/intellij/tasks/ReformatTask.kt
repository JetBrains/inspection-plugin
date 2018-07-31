package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.*
import org.jetbrains.intellij.Analyzer
import org.jetbrains.intellij.AnalyzerParameters
import org.jetbrains.intellij.QuickFixParameters
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
open class ReformatTask : AbstractInspectionsTask() {

    /**
     * {@inheritDoc}
     *
     * The sources for this task are relatively relocatable even though it produces output that
     * includes absolute paths. This is a compromise made to ensure that results can be reused
     * between different builds. The downside is that up-to-date results, or results loaded
     * from cache can show different absolute paths than would be produced if the task was
     * executed.
     */
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource() = super.getSource()!!

    /**
     * Destination directory for reformat result. Defaults to <tt>null</tt> means that reformat will be in-place.
     */
    @get:OutputDirectory
    @get:Optional
    var destinationReformatDirectory: File?
        get() = extension.destinationReformatDirectory
        set(value) {
            extension.destinationReformatDirectory = value
        }

    /**
     * Quick fixes are to be executed if found fixable code style errors. Defaults to <tt>true</tt>.
     */
    @get:Input
    @get:Optional
    var hasAutoReformat: Boolean
        get() = extension.hasAutoReformat
        set(value) {
            extension.hasAutoReformat = value
        }

    /**
     * Whether or not this task will ignore failures and continue running the build.
     *
     * @return true if failures should be ignored
     */
    @Input
    override fun getIgnoreFailures() = super.getIgnoreFailures()

    override fun getAnalyzerParameters() = AnalyzerParameters(null, QuickFixParameters(destinationReformatDirectory, hasAutoReformat))

    override fun createAnalyzer(loader: ClassLoader): Analyzer {
        val className = "org.jetbrains.idea.inspections.ReformatInspectionRunner"
        @Suppress("UNCHECKED_CAST")
        val analyzerClass = loader.loadClass(className) as Class<Analyzer>
        val projectPath = project.rootProject.projectDir.absolutePath
        val mode = extension.testMode
        val analyzer = analyzerClass.constructors.first().newInstance(projectPath, mode)
        return analyzerClass.cast(analyzer)
    }
}