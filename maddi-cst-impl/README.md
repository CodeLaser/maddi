# maddi-cst-impl

The implementation of the Common Syntax Tree defined in `maddi-cst-api`: the concrete node
classes, the `RuntimeImpl`/`FactoryImpl` facade, constant/expression evaluation
(`impl/expression/eval`), and the Java printer.

Key entry point: `org.e2immu.language.cst.impl.runtime.RuntimeImpl`.

Kotlin printing lives separately in `maddi-cst-print-kotlin`; the language-neutral formatting
engine in `maddi-cst-print`. See the repository-level [`ARCHITECTURE.md`](../ARCHITECTURE.md)
for the full module map.
