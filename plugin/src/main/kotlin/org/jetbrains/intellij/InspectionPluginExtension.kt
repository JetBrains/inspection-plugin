package org.jetbrains.intellij

import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CodeQualityExtension
import java.io.File

open class InspectionPluginExtension(private val project: Project) : CodeQualityExtension() {

    /**
     * The configuration file name to use. Replaces the `configFile` property.
     */
    @get:Incubating
    @set:Incubating
    lateinit var config: String

    /**
     * The maximum number of errors that are tolerated before breaking the build
     * or setting the failure property. Defaults to <tt>0</tt>.
     *
     *
     * Example: maxErrors = 42
     *
     * @return the maximum number of errors allowed
     */
    var maxErrors: Int = 0

    /**
     * The maximum number of warnings that are tolerated before breaking the build
     * or setting the failure property. Defaults to <tt>Integer.MAX_VALUE</tt>.
     *
     *
     * Example: maxWarnings = 1000
     *
     * @return the maximum number of warnings allowed
     */
    var maxWarnings = Integer.MAX_VALUE

    /**
     * Whether rule violations are to be displayed on the console. Defaults to <tt>false</tt>.
     *
     * Example: quiet = true (do NOT display rule violations)
     */
    var isQuiet = false

    /**
     * Normally false. Value of true is used in tests to prevent IDEA shutdown.
     */
    var testMode = false

    /**
     * Path to other configuration files. By default, this path is `$projectDir/config/inspections`
     *
     */
    @get:Incubating
    @set:Incubating
    lateinit var configDir: File

    var ideaPlugins: Array<String> = emptyArray()
}