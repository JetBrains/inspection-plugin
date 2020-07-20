package org.jetbrains.intellij.extensions

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.util.*

class InspectionsExtension {

    /**
     * The maximum number of emitted inspections problems that are tolerated before breaking the build
     * or setting the failure property. <tt>null</tt> value means that that there are no limits on the
     * number of problems.
     */
    @Input
    @Optional
    var max: Int? = null

    /**
     * Registered inspections settings
     */
    @Internal
    val inspections: Map<String, InspectionExtension> = HashMap()

    /**
     * Needed for supporting configurations defined in groovy style
     */
    @Suppress("unused")
    fun max(value: Int) {
        max = value
    }
}