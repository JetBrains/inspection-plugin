// inheritFromIdea = true

final class My

interface Your {
    fun foo()
}

open class His : Your {
    open override fun foo() {}
}

// :3:1: Redundant modality modifier
// :10:5: Redundant modality modifier
// :3:13: Class ''My'' is never used
// :9:12: Class ''His'' is never used