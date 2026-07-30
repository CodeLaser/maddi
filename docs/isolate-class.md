# IsolateClass: lifting a whole type out of its project, 2026-07-28

`IsolateClass` takes one type and emits a small standalone source tree that parses and compiles against the JDK
alone: the type itself with its members' bodies verbatim, and everything it references reduced to stubs, one
compilation unit per stubbed type in the package the original came from.

Status on closed-core's hundred largest types: **100 of 100 isolated, 94 of 100 trees parse back completely,
21,299 of 21,305 compilation units (99.97%)**. The six that do not are one known cause, §5.

---

## Why a project, and not one file like IsolateMethod

This is the whole design, and it is worth stating before anything else.

`IsolateMethod` has a single compilation unit to work with, so every stub has to be nested inside the frame:
either at its top level, where its simple name is in scope, or in a chain of namespace stubs, where only its
fully-qualified name is. **One declaration satisfies one spelling**, the verbatim text dictates which, and
choosing wrong drops the unit. That single constraint produced `namespaceStub`, `nestingWouldHide`,
`frameSimpleNameClaims`, `OutOfScopeQualified`, the placement evidence and its probe round — and issues 2 and 4
of the isolate hardening round.

A project has packages. `one.Entry` and `two.Entry` coexist, each in its own unit; `Outer.Inner` keeps the
nesting it really has; a fully-qualified reference resolves through the package and a simple one through an
import, exactly as in the original. None of that machinery exists here, and none of those defects can occur.

What did carry over is **simple-name arbitration**. Two stubs, or a stub and a JDK type, can want the same
simple name in the isolated unit, and only one can have it. That rule — the verbatim text cannot be respelled,
so the type it names simply keeps the import, and the losers are vetoed into fully-qualified form — now appears
in three places, and every one of them was a corpus defect first.

## What it emits

```
a/b/X.java          the isolated type: package a.b, verbatim member bodies, its real supertypes
p/q/Value.java      one unit per stubbed dependency, public, in the original's package
r/s/Helper.java
```

- **Stubs are public**, and nested stubs public static. A stub nested in a frame may be package-private;
  one in its own package may not, because every reference to it crosses a package boundary
  (`IsolationCore.stubsCrossPackageBoundaries`).
- **Member types keep their real nesting**, so `Outer.Inner` and an imported bare `Inner` both resolve.
- **JDK types are kept as themselves** and imported — the same `partOfJdk` (jmod, not jar) rule as
  `IsolateMethod`. That is the point: the tree needs no external jar.
- **Never a star import.** See §2.

## How to run it against a corpus

`jfocus-refactor-service`, module `codelaser-refactor-extractmodule`, class `TestIsolateClosedCoreClasses`. That
repo includes maddi with `includeBuild("../maddi")`, so a maddi working-tree change reaches the driver with no
publish step.

```bash
gradle :codelaser-refactor-extractmodule:test --tests '*TestIsolateClosedCoreClasses' \
    -Dtest.split.isolateClasses=1 -Dtest.split.classTop=100 -PtestMaxHeap=16G
```

About two minutes for a hundred types; the 3M-line project parse dominates, and verification (a hundred small
parses) barely registers. Knobs: `-Dtest.split.classTop`, `-Dtest.split.classMinStmts`, `-Dtest.split.classDir`.

**Read the report out of the JUnit XML**, not the console — gradle does not forward the test's stdout:

```bash
python3 -c "
import re,html,glob
f=glob.glob('codelaser-refactor-extractmodule/build/test-results/test/*TestIsolateClosedCoreClasses.xml')[0]
out=html.unescape(re.search(r'<system-out>(.*)</system-out>', open(f).read(), re.S).group(1))
for l in out.split('\n'):
    m=re.search(r'closed-core-class-isolates/([^ ]+) \((unresolved symbol|error)\): (.*)', l)
    if m: print(f'{m.group(3)[:56]:58s} {m.group(1)}')
"
```

Three things this corpus taught, the hard way:

- **A unit whose symbols do not resolve is dropped with a warning**, never reported as a parse exception.
  Counting `parseExceptions()` reports a clean corpus that has silently lost files. The check is a
  set-difference between what was written and what came back.
- **Read the reasons, not the totals.** Twice the tree count sat still across a round while the causes
  underneath were being cleared, and once (round 12) the totals were identical while every cause changed.
- **A fixed error reveals the next one.** Producing the four log4j trees for the first time immediately
  exposed a defect that had been hiding behind the failure to produce them at all.

## The defects the corpus found

Twelve rounds, eleven defects. **All but two were in `IsolationCore`**, so `IsolateMethod` inherits them; the
catch-clause and `super.` gaps were latent there too, its corpus of single methods simply never reaching them.

### 1. Two silent no-ops (189 units)

`addDefaultConstructorsWhereExtended` did nothing at all, for two independent reasons each hiding the other. It
read the hierarchy from `stub.parentClass()`, which is not readable before the stub is committed, so its
"is extended" set was always empty; and it then asked `stub.builder().constructors()`, which is always empty
here, because `TypeInfo.Builder.addMethod` files everything under `methods()` and `ensureMethodInfo` adds
constructor stubs with `addMethod`.

The pass ran, logged nothing, and changed nothing. **The unit test written for it passed** — because it did not
reproduce the corpus case: the stub had no constructors at all, so the implicit no-arg one worked. Since then,
every new test here is checked to *fail* with its fix disabled before being trusted.

### 2. Ambiguous on-demand imports (4 trees)

closed-core declares its own `com.example.core.general.util.ArrayList`, so an isolated unit carried
`import java.util.*` beside `import com.example.core.general.util.*` and every bare `ArrayList` in the
verbatim body was ambiguous. **Vetoing a claimant does not help**: the collapse offers the name implicitly
whether or not that type is in the import list. A class isolate therefore never collapses to a star import
(`NEVER_COLLAPSE_TO_STAR`), which makes the arbitration of §3 decisive.

### 3. Simple-name arbitration, three times over

- two stubs (`one.Entry` / `two.Entry`);
- a stub against a JDK type (the custom `ArrayList` against `java.util.ArrayList`) — so both are arbitrated in
  **one** merged pass, not two;
- two library stubs both named simply (`org.junit.Assert` and `org.assertj.core.api.Assert`), where the
  named-simply heuristic cannot separate them and alphabetical order picked assertj's `Assert`, which has no
  `assertEquals`. Settled by consulting **the original file's own single-type imports**: it compiled, so what it
  imported is what that simple name meant there.

### 4. Everything else, in one line each

- **Nested types were never imported** — the candidates were restricted to primary stubs, so
  `new RowFilter<...>()` for a member type got no `import a.b.Outer.Inner`. Five differently-named
  failures, one predicate.
- **Same-package nested types need an import too.** Sharing a package puts a *top-level* type's simple name in
  scope, never a nested one's. Fixed in `ImportComputerImpl` — but **only for a type the caller asks for
  explicitly**. Granting it unconditionally broke splitclass, which prints such a type qualified and wants no
  import (`Front.Helpers` became `import a.b.Front.Helpers` plus a bare `Helpers`); granting it only to the
  isolated unit then cost 20 of the 94 trees, because stub units are ordinary printing and ask for nothing.
  `IsolateClass.printUnit` therefore asks, for every unit it writes. The distinction that matters is not
  isolated-vs-stub but *does the caller paste verbatim text the computer cannot read* — the isolators do.
- **Inherited fields were thrown away** by a guard meant to suppress duplicates of the type's own fields.
- **A stub extended by another stub needs a no-arg constructor**, or the subclass's implicit `super()` fails.
- **A type named only in a `catch` clause** is reached no other way — the body mentions the variable, never the
  type. `MyVisitor` had no `TryStatement` case at all.
- **An anonymous class never reached its supertype**: an interface has no constructor, so the branch that stubs
  the constructed type never ran for `new Comparator<X>() {...}`.
- **`super.m()` was routed to the isolated type**, which usually declares a method of that very name — that
  being why the body writes `super.` — leaving the supertype stub without it.
- **A functional interface must keep its single abstract method**, or a lambda has nothing to target.
  `ensureMethodInfo` deliberately makes interface methods `default`, which is exactly what makes the interface
  non-functional; the SAM is reproduced abstract instead.
- **Two stub methods whose erased signatures coincide** cannot both be declared — reuse the one already there.
- **A shadowed simple name must be printed qualified.** log4j's
  `AbstractOutputStreamAppender.Builder extends AbstractAppender.Builder` emitted `extends Builder<B>`, a class
  extending itself. Being excluded from the import list is *not* the same as being printed correctly.

## 5. Fluent chaining through a self-type generic — RESOLVED 2026-07-30

Six units, all of one shape: `assertThat(x).isBetween(a, b)` and `FileAppender.newBuilder().withLayout(...)`.
The receiver's type is a type parameter bounded by the type itself — `SELF extends Assert<SELF, ACTUAL>`,
`B extends Builder<B>` — so the chained method is not found on the stub.

This was written up as **the one remaining cause and the only one I would call hard**, on the reading that it was
the fifth thing this corpus reduced to *recursive generics*, after `eraseOutOfScope`, the erasure clash, the
"already committed" owner and the shadowed `Builder` name. That reading was wrong, and the way it was wrong is
worth keeping: the generics are what the *symptom* is made of, not what the defect is. The defect is **placement**
— a called method was stubbed on the type the call went THROUGH rather than on the type that DECLARES it, so on a
subtype the declaring type's parameter is out of scope and `eraseOutOfScope` does what it is supposed to do. The
fix is one helper, `IsolationCore.MyVisitor.declaringOwner`, mirroring what the `FieldReference` branch had been
doing for inherited fields all along; it took the corpus to **100 of 100 trees and 21452 of 21452 units parsing
back**. See `handoff-isolateclass-enum-and-generic-stubs.md` §3 and §7a.

The judgement call that followed — "six units in 21,305 … worth fixing?" — was therefore also answered wrongly,
for the same reason: the cost was mis-estimated because the cause was.

## State, so a later run can tell drift from regression

Measured 2026-07-28, top 100 closed-core types by total statement count:

| | |
|---|---|
| trees produced / requested | **100 / 100** |
| isolation failures | **none** |
| trees fully parsing back | **94 / 100** |
| compilation units parsing back | **21,299 / 21,305** |
| distinct remaining causes | 1 (§5) |
| runtime | ~2 min, 16G |
| `IsolateMethod` + `IsolateClass` unit tests | 63, 0 failures |
| whole maddi `test` | green |

Re-measured 2026-07-30, same knobs, after §5 and the two defects of
`handoff-isolateclass-enum-and-generic-stubs.md`. The unit total differs from the row above because closed-core
itself has moved on, so read the *ratios*, not the deltas — and note that **parsing back is the weaker of the two
gates**: the corpus below parses whole and 34 of its trees still do not compile, which is what
`TestIsolateClosedCoreClasses.MAX_TREES_NOT_COMPILING` is for.

| | |
|---|---|
| trees produced / requested | **100 / 100** |
| trees fully parsing back | **100 / 100** (was 93 on this parse before the fixes) |
| compilation units parsing back | **21452 / 21452** (was 21445) |
| trees that COMPILE | **66 / 100** (was 57) |
| largest cause left | 13 trees, "is not abstract and does not override" |
| runtime | ~2.5 min, 16G |
