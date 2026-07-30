# `DetailedSources.qualifier` returned the declaring type, not the written one

Reported 2026-07-30 from `jfocus-refactor-service` splitclass, **fixed the same day**.

---

## The defect

`ClassSymbolScanner.iterateUpToPackageLevel` recorded the qualifier of a nested type by climbing the
**resolved** type's enclosing chain, and filed each step at the position of whatever token was actually
written:

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

`Entry` resolves to `java.util.Map.Entry`, whose enclosing type is `java.util.Map`. So the scanner recorded
**`java.util.Map` at the span of the token `HashMap`**. Two consequences:

- `DetailedSources.qualifier(java.util.Map.Entry)` returned `java.util.Map`, though its javadoc says *"the
  explicitly written qualifier type"*. There was no way to tell `Map.Entry` and `HashMap.Entry` apart through
  the API.
- `Element.TypeReference.typeToImport()` follows from the same data, so an import computer that re-emitted the
  member imported `java.util.Map` — correct for the CST, wrong for the text.

It surfaced in splitclass, which copies a member's text into a new type and computes that type's imports from
the CST. A part that received the loop above imported `java.util.Map` and kept the text `HashMap.Entry`; javac
then read the qualifier as a package and said `package HashMap does not exist`. Four errors over three of the
hundred class-isolate trees — it only bites when the receiving part does not name `HashMap` for some other
reason.

## The fix

Three changes, because two consumers want different things from the same reference.

**1. `ClassSymbolScanner.iterateUpToPackageLevel` records the WRITTEN type.** `ti` still backtracks over the
declaring chain — that is what decides how many qualification steps there are and where the walk stops — but
the type filed at each step now comes from `expression.type`, which is a `Type.ClassType` for the qualifier
as written. It falls back to the backtracked type when javac has no class type there.

**2. `DetailedSourcesImpl.qualifier` reads those recordings instead of computing.** It used to do arithmetic on
the declaring chain (subtract each simple name's length from the span until it fits), which can only ever
produce declaring types. It now recovers the qualification from the prefixes recorded beside the reference:
they all BEGIN where the reference begins and each is strictly shorter, so the immediate qualifier is the
longest strictly-shorter type among them, and a package among them says the reference was written out in full
and needs no import. The old arithmetic remains as the fallback for references that carry no recorded prefix.

Recovering full qualification from the recorded package, rather than from `posDiff >= fullyQualifiedName()`,
also fixes a smaller bug in passing: `a.b.Sub.C` where `C` is declared in `a.b.Base` used to return `a.b.Base`
as a qualifier to import, though the text needs no import at all.

**3. `ImportComputerImpl` imports the written qualifier AND the declaring one, when they differ.** This is the
part that is easy to miss. Verbatim text says `HashMap.Entry` and needs `java.util.HashMap`; a printer renders
the same type down its declaring chain as `Map.Entry` and needs `java.util.Map`. Before the fix the printer's
name was the only one imported, so importing only the written qualifier turned a text bug into a printing bug:
`Map.Entry` under `import java.util.HashMap` — which is how the regression was caught, and it is silent under
any test that happens to name `Map` elsewhere. One of the two imports is redundant for any single consumer,
and which one depends on the consumer, so both are emitted.

Not fixed, and not needed by anything today: a printer still cannot reproduce the author's spelling. That would
mean carrying the written qualifier per OCCURRENCE into the print path, where `QualificationImpl` holds one
answer per compilation unit.

## What it cost elsewhere

- `TestFullyQualified.test4` changed and is the clearest statement of the new rule: for `II.J`, where `J` is
  declared in `I` and inherited into `II`, the qualifier's span now records `X.II` and `detail(X.I)` is null.
  The old expectation carried the comment `// showing as II` next to an assertion on `X.I`.
- The jfocus workaround is gone (`ReplaceData.editInheritedQualifierReferences`, which detected the anomaly by
  span and re-printed the reference from the CST). `create/TestTypeWrittenThroughInheritingQualifier` now pins
  the fixed behaviour: the part keeps `HashMap.Entry`.

## Verification

- maddi `test`: green.
- `:codelaser-refactor-extractmodule:test --tests '*splitclass*'`: green.
- splitclass class-isolate corpus: unchanged at 24 of 42 trees compiling, **12 errors in emitted files** and 670
  in pre-existing ones — the front-end fix exactly replaces the workaround it removes, and no
  `package HashMap does not exist` remains anywhere in the corpus output.
- `TestIsolateClosedCoreClasses` (the printing path, 2635 compilation units of closed-core): output **byte-identical**
  before and after.
