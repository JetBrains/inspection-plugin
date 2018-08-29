package org.jetbrains.intellij.extensions

import org.gradle.api.Action
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.Project

open class InspectionPluginExtension(private val project: Project?) : CodeQualityExtension() {

    /**
     * Run inspection Plugin in test mode
     */
    var testMode: Boolean? = null

    /**
     * Configuration of IDEA.
     */
    var idea = IdeaExtension()

    /**
     * Whether rule violations are to be displayed on the console.
     */
    var isQuiet: Boolean? = null

    /**
     * If this value is <tt>true</tt> implementation of inspections will be found in IDEA
     * profile with given {@profileName}.
     */
    var inheritFromIdea: Boolean? = null

    /**
     * Name of profiles in IDEA. Needed for finding implementation of inspections.
     */
    var profileName: String? = null

    /**
     * Error inspections configurations.
     * @see InspectionsExtension
     */
    val errors = InspectionsExtension()

    /**
     * Warning inspections configurations.
     * @see InspectionsExtension
     */
    val warnings = InspectionsExtension()

    /**
     * Info inspections configurations.
     * @see InspectionsExtension
     */
    val info = InspectionsExtension()

    /**
     * Reformat task configurations.
     * @see ReformatExtension
     */
    val reformat = ReformatExtension()

    /**
     * Configurations of IDEA plugins.
     * @see PluginsExtension
     */
    val plugins = PluginsExtension()

    /**
     * @see InspectionsExtension.max
     */
    @Deprecated("To be replaced with errors.max = n", ReplaceWith("errors.max"))
    var maxErrors: Int?
        get() = errors.max
        set(value) {
            errors.max = value
        }

    /**
     * @see InspectionsExtension.max
     */
    @Deprecated("To be replaced with warnings.max = n", ReplaceWith("warnings.max"))
    var maxWarnings: Int?
        get() = warnings.max
        set(value) {
            warnings.max = value
        }

    /**
     * @see IdeaExtension.version
     */
    @Deprecated("To be replaced with idea.version", ReplaceWith("idea.version"))
    override fun getToolVersion() = idea.version

    /**
     * @see IdeaExtension.version
     */
    @Deprecated("To be replaced with idea.version = n", ReplaceWith("idea.version"))
    override fun setToolVersion(value: String?) {
        idea.version = value
    }

    @Suppress("unused")
    fun error(name: String): InspectionExtension {
        return (errors.inspections as MutableMap).getOrPut(name) {
            InspectionExtension()
        }
    }

    @Suppress("unused")
    fun error(name: String, action: Action<InspectionExtension>) {
        val extension = error(name)
        action.execute(extension)
    }

    @Suppress("unused")
    fun warning(name: String): InspectionExtension {
        return (warnings.inspections as MutableMap).getOrPut(name) {
            InspectionExtension()
        }
    }

    @Suppress("unused")
    fun warning(name: String, action: Action<InspectionExtension>) {
        val extension = warning(name)
        action.execute(extension)
    }

    @Suppress("unused")
    fun info(name: String): InspectionExtension {
        return (info.inspections as MutableMap).getOrPut(name) {
            InspectionExtension()
        }
    }

    @Suppress("unused")
    fun info(name: String, action: Action<InspectionExtension>) {
        val extension = info(name)
        action.execute(extension)
    }

    @Suppress("unused")
    fun errors(action: Action<InspectionsExtension>) {
        action.execute(errors)
    }

    @Suppress("unused")
    fun warnings(action: Action<InspectionsExtension>) {
        action.execute(warnings)
    }

    @Suppress("unused")
    fun info(action: Action<InspectionsExtension>) {
        action.execute(info)
    }

    @Suppress("unused")
    fun reformat(action: Action<ReformatExtension>) {
        action.execute(reformat)
    }

    @Suppress("unused")
    fun idea(action: Action<IdeaExtension>) {
        action.execute(idea)
    }

    @Suppress("unused")
    fun plugins(action: Action<PluginsExtension>) {
        action.execute(plugins)
    }
}