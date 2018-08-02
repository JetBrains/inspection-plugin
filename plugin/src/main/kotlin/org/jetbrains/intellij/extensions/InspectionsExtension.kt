package org.jetbrains.intellij.extensions

import org.gradle.api.Action
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.Project

open class InspectionsExtension(private val project: Project?) : CodeQualityExtension() {

    /**
     * Value of true is used in tests to prevent IDEA shutdown.
     */
    var testMode: Boolean? = null

    /**
     * Version of IDEA.
     */
    var ideaVersion: String? = null

    /**
     * Version of IDEA Kotlin Plugin.
     */
    var kotlinPluginVersion: String? = null

    /**
     * Whether rule violations are to be displayed on the console.
     */
    var isQuiet: Boolean? = null

    /**
     * Quick fix are to be executed if found fixable errors.
     */
    var quickFix: Boolean? = null

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
}