# Discrepancies between the openjdk and maddi (congocc) parsers

Running record of places where the two source parsers produce different results for the
same input. They should agree.

- **openjdk parser**: `maddi-java-openjdk`, `ScanCompilationUnit` (javac-based).
- **maddi parser**: `maddi-java-parser` (congocc-based), used by `maddi-inspection-integration`.

---

## 2026-06-27 — `source().index()` of methods and formal parameters

Input used for the comparison:

```java
package a.b;
class C {
  int m(int x, String y) {
    int z = x;
    return z;
  }
}
```

| element            | openjdk | maddi (congocc) | should be |
|--------------------|---------|-----------------|-----------|
| method body block  | `""`    | `""`            | `""`  ✓ consistent |
| statement          | `"0"`   | `"0"`           | `"0"` ✓ consistent |
| **method**         | `"-"`   | `""`            | `"-"` ✗ **mismatch** |
| **formal parameter** | `"-"` | `""`            | `"-"` ✗ **mismatch** |

### Consistent (no action)
- **Method body** is `""` in both, by design — congocc passes `""` explicitly
  (`ParseHelperImpl:129` → `parsers.parseBlock().parse(context, "", null, codeBlock, …)`, same for
  lambda bodies at `ParseLambdaExpression:129`); openjdk reaches `""` after the block-index sentinel
  cleanup (`blockSource`/`parseBlock` now use `""` instead of the old `"-"`).
- **Statement** indices are `"0"`, `"1"`, … in both (`ParseBlock:48` `index.isEmpty() ? "" : index + "."`
  + pad, vs openjdk `statementIndex()`).

### Mismatch — method and formal parameter sources
- **maddi/congocc gives `""`.** `ParseMethodDeclaration` builds the method source with `source(md)`
  (line 244) and each parameter with `source(fp)` (line 302); both go through the no-index helper
  `CommonParse.source(Node)` (`CommonParse.java:113-114`) which hard-codes the index to `""`.
- **openjdk gives `"-"`.** Method/parameter sources fall through to the `"-"` default
  (`sourceForNode(node, "-")` / `newParserSource("-", …)`, `ScanCompilationUnit` ~2646/2636) — the
  "no statement context" marker.

Per the agreed convention (method + formal parameters = `"-"`, body = `""`), **openjdk is correct**
and the **congocc parser is diverging** (uses `""` where it should use `"-"`).

### Suggested fix (not yet applied)
Localized to `ParseMethodDeclaration`:
- method: `source("-", md)` instead of `source(md)` (line 244);
- parameter: `source("-", fp)` instead of `source(fp)` (line 299/302).

Body and statements already match and must not be touched. Do **not** change the `CommonParse.source(Node)`
default of `""` — it is used widely and would ripple far beyond methods/parameters.

---

## 2026-06-29 — constructor statement indices shifted by the synthetic `super()`  (RESOLVED)

Input: `class X { int a, b; X(int x, int y) { a = x; b = y; } }`.

- **maddi/congocc**: no implicit `super()` statement; `a = x` at index `0`, `b = y` at `1`.
- **openjdk (before)**: javac inserts a synthetic `super();` (index `null`, `isSynthetic()`) at list slot 0, so the
  real statements were numbered from `1` (`a = x` → `1`, `b = y` → `2`). This broke `TestAssignmentsConstructor`
  under the openjdk parser.

**Fix (openjdk, `ScanCompilationUnit.statementIndex()`):** a leading synthetic `ExplicitConstructorInvocation`
no longer counts towards the statement position (nor the padding width), so real statements start at `0`, matching
maddi. The synthetic `super()` itself remains in the body (with its null index) — records and the call graph rely
on it — it simply does not shift the index of the written statements.

## 2026-06-29 — NPE sorting fields with a null (synthetic) source  (RESOLVED)

Input: the `@GetSet` interface in `TestGetSet` test4 (`@GetSet Function ... function();` etc.).

- Synthetic `@GetSet` backing fields are created on the interface during scanning and have **no source**.
- **openjdk (before)**: `continueType` sorts members for code reproduction
  (`builder.fields().sort(Comparator.comparing(FieldInfo::source))`); the null source made `Comparator.comparing`
  throw an NPE, aborting the whole compilation unit. maddi does not perform this sort, so it never crashed.

**Fix (openjdk, `ScanCompilationUnit.continueType`):** the method/field/sub-type sorts use
`Comparator.nullsLast(...)`, so source-less synthetic members sort last instead of crashing.
