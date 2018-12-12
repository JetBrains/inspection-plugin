package org.jetbrains.idea.inspections.runners

class RunnerException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}