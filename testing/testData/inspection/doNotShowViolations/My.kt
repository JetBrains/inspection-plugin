// warning: org.jetbrains.kotlin.idea.inspections.CanBeParameterInspection
// quiet = true

class My(val x: Int) {
    val y = x
}

// SHOULD_BE_ABSENT
// :4:10: Constructor parameter is never used as a property