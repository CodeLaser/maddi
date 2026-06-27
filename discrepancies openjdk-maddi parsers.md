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
