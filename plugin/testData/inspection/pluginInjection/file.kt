// warning: org.jetbrains.kotlin.idea.inspections.CanSealedSubClassBeObjectInspection

sealed class A {
    class B : A()
    class C : A()
}

// :4:4:warning
// :5:4:warning
