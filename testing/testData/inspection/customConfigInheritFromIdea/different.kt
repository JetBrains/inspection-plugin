// inheritFromIdea = true
// config = inherit.xml

final class My

interface Your {
    fun foo()
}

open class His : Your {
    open override fun foo() {}
}

// :4:1: Redundant modality modifier
// :11:5: Redundant modality modifier
// :4:13: Class "My" is never used
// :10:12: Class "His" is never used