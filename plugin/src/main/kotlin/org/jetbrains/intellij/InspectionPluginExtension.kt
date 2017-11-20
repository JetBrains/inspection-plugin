package org.jetbrains.intellij

import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.resources.TextResource
import java.io.File
import java.util.LinkedHashMap

open class InspectionPluginExtension(private val project: Project) : CodeQualityExtension() {

    /**
     * The configuration to use. Replaces the `configFile` property.
     */
    @get:Incubating
    @set:Incubating
    lateinit var config: TextResource

    /**
     * The properties available for use in the configuration file. These are substituted into the configuration file.
     */
    var configProperties: Map<String, Any> = LinkedHashMap()

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
     * Whether rule violations are to be displayed on the console. Defaults to <tt>true</tt>.
     *
     * Example: showViolations = false
     */
    var isShowViolations = true

    /**
     * Path to other configuration files. By default, this path is `$projectDir/config/inspections`
     *
     */
    @get:Incubating
    @set:Incubating
    lateinit var configDir: File

    /**
     * The configuration file to use.
     */
    var configFile: File
        get() = config.asFile()
        set(configFile) {
            config = project.resources.text.fromFile(configFile)
        }

    var ideaPlugins: Array<String> = emptyArray()
}