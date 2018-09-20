// warning: org.jetbrains.kotlin.idea.inspections.KotlinCleanupInspection
// warning: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection

import java.io.File

fun main(args: Array<String>) {
    test(args[0])
}

fun test(inputName: String) {
    File(inputName).readLines().firstOrNull()?.foo()
}

fun String.foo() {
    hashCode()
}

// SHOULD_BE_ABSENT
// :4:1: Remove deprecated symbol import
// :11:46: Unnecessary safe call on a non-null receiver of type [ERROR : <ERROR FUNCTION RETURN TYPE>]
// :14:12: Function "foo" is never used

