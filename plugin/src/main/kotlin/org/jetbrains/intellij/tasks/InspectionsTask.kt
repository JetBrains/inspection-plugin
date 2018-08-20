package org.jetbrains.intellij.tasks

import org.gradle.api.plugins.quality.CheckstyleReports
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.*
import org.gradle.api.Project as GradleProject

@CacheableTask
open class InspectionsTask : AbstractInspectionsTask(), Reporting<CheckstyleReports>
