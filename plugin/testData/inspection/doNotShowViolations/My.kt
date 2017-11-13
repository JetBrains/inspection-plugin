// warning: org.jetbrains.kotlin.idea.inspections.CanBeParameterInspection
// showViolations = false

class My(val x: Int) {
    val y = x
}

// SHOULD_BE_ABSENT
// :2:10: Constructor parameter is never used as a property