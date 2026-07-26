# `<nulltype>` at `ClassSymbolScanner.convert`: javac's marker for an unresolvable annotation element

**Investigated and fixed:** 2026-07-26, from `ws/std2`, answering
[`docs/handoff-nulltype-classsymbolscanner.md`](../../docs/handoff-nulltype-classsymbolscanner.md)
(issues 2 and 3 of `docs/handoff-from-jfocus-standardize.md`).
**Status:** cause measured end to end, fixed here, root cause fixed in the jfocus test fixture.
**Corrects** three readings in the handoff — see [§5](#5-where-the-handoff-was-wrong-and-why-it-could-not-have-known).

## 1. Summary

`convert(Type, Set<Type>)` fell through to `throw new UnsupportedOperationException("NYI")` for javac's
`Type.BottomType`. The handoff read that as "the type of the `null` literal, a real type maddi has no case for",
and proposed returning `ParameterizedTypeImpl.NULL_CONSTANT`.

Measured, it is neither the type of `null` nor a parser gap. Every occurrence is **javac's placeholder for an
annotation element it could not resolve**, and the answer is to fix the class path.

## 2. The chain, measured

1. A **class file** carries an annotation with an **enum-constant** value, e.g. junit's
   `@org.apiguardian.api.API(status = Status.STABLE)`.
2. The enum's own class file (`org.apiguardian.api.API$Status`) is **not on the class path**.
3. javac's `ClassReader.AnnotationDeproxy.visitEnumAttributeProxy` (JDK 26, `ClassReader.java` ≈`:2135-2161`)
   cannot find the constant, so it substitutes a placeholder and reports a **warning**:

   ```java
   log.warning(Warnings.UnknownEnumConstant(currentClassFile, enumTypeSym, proxy.enumerator));
   result = new Attribute.Enum(enumTypeSym.type,
           new VarSymbol(0, proxy.enumerator, syms.botType, enumTypeSym));
   //                                        ^^^^^^^^^^^^^ the <nulltype>
   ```

   (`ClassReader.java:2085` does the same with a `botType` *return type* for an annotation method that is not
   found — same idea, different member.)
4. `MaddiDiagnosticCollector.report` records only `Diagnostic.Kind.ERROR`, so that warning is **discarded**.
   This is why the parse reports no errors while hundreds of these fire.
5. maddi walks into it from the annotation reader (all in `ClassSymbolScanner`; the line numbers are the
   pre-fix ones the handoff cites, since this change moved them):

   ```
   loadAnnotations(symbol)                    :509
     annotationExpression(compound)           :528
       annotationValue(Attribute.Enum en)     :544  -> getOrLoadField(en.value)
         ensureField(vs)                      :924  -> convert(vs.type)  // vs.type == syms.botType
           convert(Type, Set<Type>)                 -> no case -> throw at :1064
   ```
6. `annotationValue`'s blanket `catch (RuntimeException re) { return null; }` (`:567` pre-fix) swallows it and
   the key/value pair is dropped — **silently**, since step 4 already discarded javac's own account of it.

No expression-type caller was observed to receive a bottom type, in any run measured here. That is consistent
with the shapes that carry one: scanning javac's trees for tag `BOT` over the whole fernflower corpus and over
constructed samples turns up only `NULL_LITERAL`, `CONDITIONAL_EXPRESSION` with two `null` arms, and
`PARENTHESIZED` wrapping one of those — and maddi converts none of those types. It handles the `null` literal at
`ScanCompilationUnit:2283` (`case BOT -> runtime.newNullConstant(...)`) without asking `convert` for a type, and
`visitConditionalExpression` builds its result from the arms rather than from `node.type`. Not a proof of
unreachability, which is why `convert` keeps throwing rather than guessing.

## 3. The fix

`ClassSymbolScanner.unresolvedEnumConstant` recognises the marker in `annotationValue`, **before** anything tries
to convert it, and skips the value deliberately with one `LOGGER.warn` per (enum, constant) naming the library to
put on the class path. The annotation is still attached; only the unresolvable value is missing.

`convert` deliberately kept **no** `BOT` case, against the handoff's §6. Returning `NULL_CONSTANT` would let
`ensureField` build a `FieldInfo` typed "type of the null constant" and then `owner.builder().addField(...)` it
onto a real binary type — a fabricated member, permanently, for a field that does not exist. A loud throw is
better than that, and the throw now says which type it choked on.

Pinned by `other/TestUnresolvedEnumConstantInAnnotation` (a fixture compiled at test time, with the enum's class
file removed afterwards), plus a control that the resolvable case still reads the value.

## 4. Numbers

Measured on `jfocus-standardize`'s `TestIntakeAttrition` / `TestFernflowerDedup`, whose fixture put
junit-jupiter-api and junit-platform-commons on the analysis class path but not apiguardian-api (junit declares
it `compileOnly`, so it is not transitive):

| | throws at `convert` | values dropped |
|---|---|---|
| before | 838 | 838, silently |
| after the maddi fix | 0 | 838, each named once per constant |
| after the class-path fix as well | 0 | 0 |

All five `org.apiguardian.api.API.Status` constants appeared: `STABLE` 624, `MAINTAINED` 134, `DEPRECATED` 44,
`EXPERIMENTAL` 24, `INTERNAL` 12. The corpus under test is irrelevant — the annotations come from the
**preloads**, so any test in that module reproduced it; "unmodified fernflower" was a coincidence of where it was
first seen.

The class-path fix is in `jfocus-transform/codelaser-transform-common`'s `testFixtures` `CommonTest`.

## 5. Where the handoff was wrong, and why it could not have known

Three readings in `docs/handoff-nulltype-classsymbolscanner.md` do not survive measurement. None of them was
careless; each follows from not being able to run the reproduction, which is exactly what that document said.

- **§2a — "`Type.BottomType`, the type of `null`. A real type."** True of `BottomType` in general, and the
  identification of the symbol was exact. But not of this population: here it is a failure marker, in the same
  family as `recoveryType`, so §6's prescription inverts.
- **§4 — "annotation values are ruled out."** The reasoning covers `Attribute.Constant` (whose `switch` on
  `getTag()` has no `BOT` case and never calls `convert`), and that part is correct. It does not cover
  `Attribute.Enum`, which reaches `convert` through `getOrLoadField` → `ensureField`. That is the whole
  population, and `annotationValue:567` — named there as "**not** the swallow site" — is the swallow site.
- **§5 — "`methodInvocation.type` is the candidate I would look at first: a generic method whose type variable
  javac infers to the null type."** javac does not do that: for `<T> T id(T t)` called as `id(null)` it infers
  `Object`, checked directly against the JDK 26 compiler — as it does for `pick(null, null)` and
  `wrap(null)`. So that population does not exist to be found.

§3's instinct was right, though — "**suspect the configuration before the parser**", and Bart's prior with it.
It expected the evidence as `Type$2` (`recoveryType`); an incomplete class path in fact yields `ErrorType`s and
javac ERROR diagnostics, and this signal instead. Same conclusion, different mechanism.

## 6. Also landed: naming what an `NYI` throw met

Issue 3 of the jfocus handoff. All 23 bare `throw new UnsupportedOperationException("NYI")` in this module now
name the value: 3 in `ClassSymbolScanner` (helper `unexpectedJavacType`) and 20 in `ScanCompilationUnit` (helper
`unexpected`). The two meanings of `NYI` — "not implemented yet" and "should be unreachable" — call for opposite
responses, and this defect is the case in point: it was reported as a parser gap, and it was a class-path
problem. Identifying the type had cost a patched build and a re-run; it now costs reading the message.
