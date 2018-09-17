// warning: org.jetbrains.kotlin.idea.inspections.AddVarianceModifierInspection

interface Comparable<T> {
    fun compareTo(other: T): Int
}

// :3:22: Type parameter can have in variance