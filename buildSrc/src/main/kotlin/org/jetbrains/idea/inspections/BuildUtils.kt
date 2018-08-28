package org.jetbrains.idea.inspections

import org.gradle.api.Project
import java.io.File

val Project.projectVersion: String
    get() = this.findProperty("version") as String

val Project.projectGroup: String
    get() = this.findProperty("group") as String

inline fun <reified C> Project.configure(name: String, configuration: C.() -> Unit) {
    (this.tasks.getByName(name) as C).configuration()
}
