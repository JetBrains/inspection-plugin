// warning: org.jetbrains.kotlin.idea.inspections.CanSealedSubClassBeObjectInspection
// plugins.kotlin.version = 1.2.60

sealed class A {
    class B : A()
    class C : A()
}

// :5:5: Sealed sub-class has no state and no overridden equals
// :6:5: Sealed sub-class has no state and no overridden equals
