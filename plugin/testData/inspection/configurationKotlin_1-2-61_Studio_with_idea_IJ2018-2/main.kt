// idea.version = ideaIC:2018.2
// plugins.kotlin.location = https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49186
// warning: org.jetbrains.kotlin.idea.inspections.ReplaceStringFormatWithLiteralInspection

val a = ""
val b = String.format("--tests \"%s\" ", a.replace('\"', '*'))

// EXCEPTION: RunnerException: Kotlin plugin cannot be loaded
