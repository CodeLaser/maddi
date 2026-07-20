# maddi-cst-api

The interface layer of the **Common Syntax Tree** (CST): the language-agnostic tree, shared by
the Java and Kotlin front ends, on which every downstream analysis operates. Defines the
element/expression/statement/type interfaces, the `TypeInfo`/`MethodInfo`/`FieldInfo` info
objects, and the central `Runtime`/`Factory` facade through which trees are constructed.

Key entry points: `org.e2immu.language.cst.api.runtime.Runtime`,
`org.e2immu.language.cst.api.element.Element`, `org.e2immu.language.cst.api.info.TypeInfo`.

Remember: info objects are single-instance per (FQN, source set) — compare with `==`.

The `kotlin-*.md` files in this module are design notes from extending the CST to host Kotlin.
See the repository-level [`ARCHITECTURE.md`](../ARCHITECTURE.md) for how this module fits the
pipeline; the implementation lives in `maddi-cst-impl`.
