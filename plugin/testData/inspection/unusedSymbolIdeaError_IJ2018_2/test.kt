// idea.version = ideaIC:2018.2
// inheritFromIdea = true
// error: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection

class My

fun foo(arg: Int) {}

val answer get() = 42

// ERROR: :5:7: Class "My" is never used
// ERROR: :7:5: Function "foo" is never used
// ERROR: :9:5: Property "answer" is never used