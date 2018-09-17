// warning: org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection
// error: org.jetbrains.kotlin.idea.inspections.CanBeValInspection
// info: org.jetbrains.kotlin.idea.intentions.FoldInitializerAndIfToElvisInspection
// warning: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection
// htmlReport = true
// maxErrors = 1000

data class My private constructor(val x: Double, val y: Int, val z: String)

fun canBeVal(): Int {
    var x = 1
    var y = x * 2
    var z = "98" + y
    return z.toInt()
}

fun foldToElvis(arg: Int?): Int {
    val n = arg
    if (n == null) return -1
    return n
}

class Unused {
    val s: String get() = ""
    fun bar(s: String) = s
}