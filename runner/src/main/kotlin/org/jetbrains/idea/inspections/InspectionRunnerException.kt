package org.jetbrains.idea.inspections

class InspectionRunnerException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}