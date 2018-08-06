package org.jetbrains.intellij.utils

import org.gradle.api.Project
import org.gradle.api.tasks.WorkResult

class Copy(private val project: Project) {
    operator fun invoke(from: Any, into: Any): WorkResult = project.copy {
        it.from(from)
        it.into(into)
    }
}