package test

data class My private constructor(
        val x: Double, val y: Int, val z: String, val w: Char
)

public val thirteen = 99

interface Comparable<T> {
    fun compareTo(other: T): Int
}