package org.jetbrains.intellij.tasks

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.plugins.quality.CheckstyleReports
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.*
import org.jetbrains.intellij.IdeaCheckstyleReports
import java.io.File
import org.gradle.api.Project as GradleProject

@CacheableTask
open class InspectionsTask : AbstractInspectionsTask(), Reporting<CheckstyleReports> {
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

    override val xml: File?
        get() = if (reports.xml.isEnabled) reports.xml.destination else null

    override  val html: File?
        get() = if (reports.html.isEnabled) reports.html.destination else null
}
