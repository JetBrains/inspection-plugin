// warning: org.jetbrains.kotlin.idea.inspections.CanBeValInspection
// maxWarnings = 2
// ignoreFailures = true

fun foo(a: Int, b: Int, c: Int): Int {
    var x = a
    var y = b
    var z = c
    return x + y + z
}

// :6:5: Variable is never modified and can be declared immutable using 'val'
// :7:5: Variable is never modified and can be declared immutable using 'val'
// :8:5: Variable is never modified and can be declared immutable using 'val'