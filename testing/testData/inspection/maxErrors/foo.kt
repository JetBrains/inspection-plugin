// error: org.jetbrains.kotlin.idea.inspections.CanBeValInspection
// maxErrors = 2

fun foo(a: Int, b: Int, c: Int): Int {
    var x = a
    var y = b
    var z = c
    return x + y + z
}

// FAIL
// ERROR: :5:5: Variable is never modified and can be declared immutable using 'val'
// ERROR: :6:5: Variable is never modified and can be declared immutable using 'val'
// ERROR: :7:5: Variable is never modified and can be declared immutable using 'val'