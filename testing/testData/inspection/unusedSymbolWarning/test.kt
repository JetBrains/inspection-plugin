// warning: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection

class My

fun foo(arg: Int) {}

val answer get() = 42

// WARNING: :3:7: Class "My" is never used
// WARNING: :5:5: Function "foo" is never used
// WARNING: :7:5: Property "answer" is never used