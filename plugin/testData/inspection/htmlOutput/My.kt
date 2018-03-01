// warning: org.jetbrains.kotlin.idea.inspections.DataClassPrivateConstructorInspection
// error: org.jetbrains.kotlin.idea.inspections.CanBeValInspection
// htmlReport = true
// maxErrors = 1000

data class My private constructor(val x: Double, val y: Int, val z: String)

fun canBeVal(): Int {
    var x = 1
    var y = x * 2
    var z = "98" + y
    return z.toInt()
}