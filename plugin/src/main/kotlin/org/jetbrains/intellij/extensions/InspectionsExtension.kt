package org.jetbrains.intellij.extensions

import org.gradle.api.Action
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.Project

open class InspectionsExtension(private val project: Project?) : CodeQualityExtension() {

    /**
     * Configuration of IDEA.
     */
    var idea = IdeaExtension()

    /**
     * Whether rule violations are to be displayed on the console.
     */
    var isQuiet: Boolean? = null

    /**
     * Quick fix are to be executed if found fixable errors.
     */
    var quickFix: Boolean? = null

    /**
     * Binary sources will not participate in the analysis.
     */
    var skipBinarySources: Boolean? = null

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
     * @see InspectionTypeExtension
     */
    val errors = InspectionTypeExtension()

    /**
     * Warning inspections configurations.
     * @see InspectionTypeExtension
     */
    val warnings = InspectionTypeExtension()

    /**
     * Info inspections configurations.
     * @see InspectionTypeExtension
     */
    val infos = InspectionTypeExtension()

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
     * @see InspectionTypeExtension.max
     */
    @Deprecated("To be replaced with errors.max = n", ReplaceWith("errors.max"))
    var maxErrors: Int?
        get() = errors.max
        set(value) {
            errors.max = value
        }

    /**
     * @see InspectionTypeExtension.max
     */
    @Deprecated("To be replaced with warnings.max = n", ReplaceWith("warnings.max"))
    var maxWarnings: Int?
        get() = warnings.max
        set(value) {
            warnings.max = value
        }

    /**
     * @see InspectionsExtension.idea.version
     */
    @Deprecated("To be replaced with idea.version", ReplaceWith("idea.version"))
    override fun getToolVersion() = idea.version

    /**
     * @see InspectionsExtension.idea.version
     */
    @Deprecated("To be replaced with idea.version = n", ReplaceWith("idea.version"))
    override fun setToolVersion(value: String?) {
        idea.version = value
    }

    @Suppress("unused")
    fun errors(action: Action<InspectionTypeExtension>) {
        action.execute(errors)
    }

    @Suppress("unused")
    fun warnings(action: Action<InspectionTypeExtension>) {
        action.execute(warnings)
    }

    @Suppress("unused")
    fun infos(action: Action<InspectionTypeExtension>) {
        action.execute(infos)
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