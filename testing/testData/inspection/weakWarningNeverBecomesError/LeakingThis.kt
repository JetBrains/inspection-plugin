// error: org.jetbrains.kotlin.idea.inspections.LeakingThisInspection

fun foo(l: LeakingThis) {}

open class LeakingThis {
    init {
        foo(this)
    }
}

// :7:13: Leaking 'this' in constructor of non-final class LeakingThis