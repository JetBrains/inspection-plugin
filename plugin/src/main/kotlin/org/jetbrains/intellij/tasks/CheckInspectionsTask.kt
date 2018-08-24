package org.jetbrains.intellij.tasks

import org.gradle.api.tasks.CacheableTask


@CacheableTask
open class CheckInspectionsTask : InspectionsTask() {
    override val isAvailableCodeChanging: Boolean
        get() = false
}