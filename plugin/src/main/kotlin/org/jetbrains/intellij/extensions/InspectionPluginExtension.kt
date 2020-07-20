package org.jetbrains.intellij.extensions

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

open class InspectionPluginExtension(
        @Suppress("unused") private val project: Project?
) : CodeQualityExtension() {

    /**
     * Configuration of IDEA.
     */
    @Nested
    var idea = IdeaExtension()

    /**
     * Whether rule violations are to be displayed on the console.
     */
    @get:Input
    @get:Optional
    var isQuiet: Boolean? = null

    /**
     * Whether temp directory (with all caches) is located in home.
     * By default, it's located in temp
     */
    @get:Input
    @get:Optional
    var isTempDirInHome: Boolean? = null

    /**
     * If this value is <tt>true</tt> implementation of inspections will be found in IDEA
     * profile with given {@profileName}.
     */
    @Optional
    @Input
    var inheritFromIdea: Boolean? = null

    /**
     * Name of profiles in IDEA. Needed for finding implementation of inspections.
     */
    @Input
    @Optional
    var profileName: String? = null

    /**
     * Error inspections configurations.
     * @see InspectionsExtension
     */
    @Nested
    val errors = InspectionsExtension()

    /**
     * Warning inspections configurations.
     * @see InspectionsExtension
     */
    @Nested
    val warnings = InspectionsExtension()

    /**
     * Info inspections configurations.
     * @see InspectionsExtension
     */
    @Nested
    val info = InspectionsExtension()

    /**
     * Reformat task configurations.
     * @see ReformatExtension
     */
    @Nested
    val reformat = ReformatExtension()

    /**
     * Configurations of IDEA plugins.
     * @see PluginsExtension
     */
    @Nested
    val plugins = PluginsExtension()

    /**
     * @see InspectionsExtension.max
     */
    @Suppress("unused")
    @Deprecated("To be replaced with errors.max = n", ReplaceWith("errors.max"))
    @get:Input
    @get:Optional
    var maxErrors: Int?
        get() = errors.max
        set(value) {
            errors.max = value
        }

    /**
     * @see InspectionsExtension.max
     */
    @Suppress("unused")
    @Deprecated("To be replaced with warnings.max = n", ReplaceWith("warnings.max"))
    @get:Input
    @get:Optional
    var maxWarnings: Int?
        get() = warnings.max
        set(value) {
            warnings.max = value
        }

    fun isTempDirInHome() = isTempDirInHome ?: false

    /**
     * @see IdeaExtension.version
     */
    @Suppress("unused")
    @Deprecated("To be replaced with idea.version = n", ReplaceWith("idea.version"))
    @get:Input
    @get:Optional
    var toolVersion: String?
        get() = idea.version
        set(value) {
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