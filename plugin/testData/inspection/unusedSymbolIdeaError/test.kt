// inheritFromIdea = true
// error: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection

class My

fun foo(arg: Int) {}

val answer get() = 42

// ERROR: :4:7: Class "My" is never used
// ERROR: :6:5: Function "foo" is never used
// ERROR: :8:5: Property "answer" is never used