package org.jetbrains.intellij.extensions

class InspectionTypeExtension {

    /**
     * The applicable inspections.
     */
    var inspections: Set<String>? = null

    /**
     * The maximum number of emitted inspections problems that are tolerated before breaking the build
     * or setting the failure property. <tt>null</tt> value means that that there are no limits on the
     * number of problems.
     */
    var max: Int? = null
}