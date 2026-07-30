# `DetailedSources.qualifier` returns the declaring type, not the written one

Reported 2026-07-30 from `jfocus-refactor-service` splitclass. Not fixed here — the file is
`ClassSymbolScanner`, which another thread is working in, so this is a description rather than a patch.

---

## The defect

`ClassSymbolScanner.iterateUpToPackageLevel` (around line 1261) records the qualifier of a nested type by
climbing the **resolved** type's enclosing chain, and files each step at the position of whatever token was
actually written:

```java
if (expression instanceof JCTree.JCIdent) {
    if (ti.compilationUnitOrEnclosingType().isRight()) {
        ti = ti.compilationUnitOrEnclosingType().getRight();
        dsb.put(ti, expressionSource);      // <-- the DECLARING type, at the WRITTEN token's position
    }
    break;
}
```

Java lets a nested type be named through any class that inherits it, and real code does:

```java
for (HashMap.Entry<Long, String> e : map.entrySet()) { ... }
```

`Entry` resolves to `java.util.Map.Entry`, whose enclosing type is `java.util.Map`. So the scanner records
**`java.util.Map` at the span of the token `HashMap`**. Two consequences:

- `DetailedSources.qualifier(java.util.Map.Entry)` returns `java.util.Map`, though its javadoc says *"the
  explicitly written qualifier type for typeInfo (e.g. `Map` when the source contains `Map.Entry`)"*. There is no
  way to tell `Map.Entry` and `HashMap.Entry` apart through the API.
- `Element.TypeReference.typeToImport()` follows from the same data, so an import computer that re-emits the
  member imports `java.util.Map` — correct for the CST, wrong for the text.

## How it surfaced

splitclass copies a member's text into a new type and computes that type's imports from the CST. A part that
received the loop above imported `java.util.Map` and kept the text `HashMap.Entry`; javac then reads the
qualifier as a package and says `package HashMap does not exist`. Four errors over three of the hundred
class-isolate trees — it only bites when the receiving part does not name `HashMap` for some other reason.

## What a fix would look like

`expression.type` is a `Type.ClassType` for the written qualifier at each step, so the written type is available
where the declaring one is being used. Recording that instead would make `qualifier()` match its documentation
and let a consumer import what the author wrote.

Blast radius is worth checking rather than assuming: everything that computes imports goes through this, and the
same is true of the `JCFieldAccess` branch a few lines below.

## What jfocus does meanwhile

`ReplaceData.editInheritedQualifierReferences` re-prints the whole reference from the CST — `HashMap.Entry`
becomes `Map.Entry` — which is import-consistent because the replacement is printed with the very
`Qualification` the imports were computed from. It detects the anomaly **by span**: the qualifier is recorded
over seven columns and `Map` is three characters long, so a recorded type whose span cannot hold its own name is
not the name written there.

That workaround is deliberately conservative and has one hole: a written qualifier that differs from the
declaring type but happens to be **the same length** is missed. Only the front-end fix catches that. Once it
lands, the jfocus rewrite becomes a no-op and can be deleted; the worked example that pins the behaviour is
`create/TestTypeWrittenThroughInheritingQualifier`.
