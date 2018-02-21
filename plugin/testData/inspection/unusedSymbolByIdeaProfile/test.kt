// inheritFromIdea = true
// maxErrors = 2
// error: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection

class My

fun foo(arg: Int) {}

val answer get() = 42

// FAIL
// :5:7: Class ''My'' is never used
// :7:5: Function ''foo'' is never used
// :9:5: Property ''answer'' is never used