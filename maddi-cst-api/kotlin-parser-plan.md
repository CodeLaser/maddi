# Building a Kotlin front-end for the CST — implementation plan

*Plan date: 2026-06-27. Goal: a third source-language front-end (after the congocc and javac Java
parsers) that turns Kotlin source into the shared CST, using the **Kotlin K2 Analysis API** as the
resolved source of truth, modelled on `maddi-java-openjdk` + `maddi-inspection-openjdk`.*

Companion document: `kotlin-cst-assessment.md` (what the CST API itself must grow to host Kotlin).

> **Workspace note:** this Kotlin effort lives in the separate clone `~/git/maddi-kotlin` on branch
> `kotlin`, to avoid colliding with concurrent work in `~/git/maddi`.

---

## 1. Decisions taken

| Question | Decision |
|---|---|
| Compiler surface | **K2 Analysis API** (`analyze {}` / `KaSession`, `KaSymbol`, `KaType`). Resolved symbols + types + nullability — the analogue of javac's `Trees`/`Types`/`Elements`. |
| First milestone | **Walking skeleton**: new module compiles, takes the compiler dependency, converts `class Foo { fun bar(): Int = 1 }` into a CST `TypeInfo`/`MethodInfo` end-to-end. |

## 2. Decisions settled (2026-06-27)

- **D1 — Bridge language is Kotlin, not Java. ✅ ACCEPTED.** The Analysis API entry point
  `analyze(element) { … }` is an inline function with a `KaSession` receiver; not realistically callable
  from Java. The scanner module `maddi-kotlin-k2` is written in **Kotlin**; the build gains the Kotlin
  Gradle plugin.
- **D2 — Module split. ✅ START COLLAPSED.** One module `maddi-kotlin-k2` for now; extract
  `maddi-inspection-kotlin` once the driver grows (M5).
- **D3 — Kotlin version. ✅ LATEST STABLE = 2.4.0.** Pinned, with its matching Standalone Analysis API
  artifacts (verified in M0).
- **D4 — Bazel. ✅ GRADLE-ONLY for the skeleton.** Add `rules_kotlin` once the module stabilises.

## 3. Architecture (target)

Both Java front-ends produce the same CST behind the same `JavaInspector` contract. The Kotlin
front-end becomes a third producer:

```
                         maddi-cst-api / -impl   (shared tree; grows per kotlin-cst-assessment.md)
                                  ▲
        ┌─────────────────────────┼─────────────────────────┐
   congocc (java-parser)     javac (java-openjdk)       K2 (kotlin-k2)        ← NEW
   inspection-integration    inspection-openjdk         inspection-kotlin     ← NEW (later; D2)
```

Mirror of the javac front-end:

| Java (javac) | Kotlin (K2) | Responsibility |
|---|---|---|
| `maddi-java-openjdk` (`ScanCompilationUnit`, `ClassSymbolScanner`, `ConvertType`, `FlagHelper`) | `maddi-kotlin-k2` | Walk the resolved compiler model → call `runtime.new…` CST factories |
| `maddi-inspection-openjdk` (`JavaInspectorImpl`, `CompiledTypesManagerImpl`) | `maddi-inspection-kotlin` | Build the standalone session, drive the scan, implement `JavaInspector` |

**Key lesson borrowed from java-openjdk:** it is a *hybrid* — javac for semantics, plus the congocc
parser re-run purely to recover surface syntax (comments, keyword offsets, `detailedSources`) that the
compiler discards. The Analysis API keeps **PSI (`KtFile`/`KtElement`) available alongside resolved
symbols**, so for Kotlin we get both from one session — no second parser needed. Use `KaSymbol` for
semantics and the symbol's `psi`/`KtElement` for source positions & detailed sources.

## 4. The factory contract the bridge calls (grounded in ScanCompilationUnit)

The bridge never constructs CST nodes directly; it calls `Runtime` factories and commits builders:

```
TypeInfo ti = runtime.newTypeInfo(compilationUnit, simpleName);
ti.builder().setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .setAccess(runtime.accessPublic())
            .addMethod(md)
            .commit();
MethodInfo md = runtime.newMethod(ti, "bar", methodType);
md.builder().setReturnType(intPt).setMethodBody(block).addMethodModifier(...).commit();
```

So each Kotlin concept becomes "find the right `runtime.new…` / `…Modifier…` call". Where the assessment
doc identifies a gap (e.g. `variance()`, extension receiver, `isSuspend()`, `PropertyInfo`), the CST API
must grow **first**, then the bridge calls the new factory.

## 5. Milestones

**M0 — De-risk the dependency (D3). ✅ DONE — see §8.** The standalone Analysis API resolves, the spike
compiles, and the session bootstraps. Approach confirmed viable.

**M1 — Walking skeleton. ✅ DONE.** `KotlinScan` (`maddi-kotlin-k2/src/main/kotlin`) builds a standalone
session, walks each top-level class symbol and its declared function symbols, and emits a committed CST
`TypeInfo`/`MethodInfo` via the `runtime.new…` factories. `KotlinScanTest` proves
`class Foo { fun bar(): Int = 1 }` → a `TypeInfo("Foo")` with `findUniqueMethod("bar",0)` whose
`returnType()` is the CST `int`. Method bodies are a stub empty block (deferred to M3); parameters,
visibility, generics deferred to M2/M4. Commit lifecycle mirrors java-openjdk: per-method
`builder().commitParameters().commit()`, then `type.builder()…commit()`, then `cu.setTypes(...)`.

**M2 — Signatures & the type layer.** *(M2 signatures + M2a nullability ✅ DONE; M2b variance + generics
+ class-type resolution remain.)* Real `ParameterizedType` conversion: parameters, return types,
generics, `java.lang`↔Kotlin builtin mapping (`kotlin.Int`→`int`, `kotlin.String`→`String`).
- **M2a nullability — DONE.** Added `NullableState {UNSPECIFIED, NONNULL, NULLABLE}` to `cst-api` and a
  `nullable()` / `withNullable()` dimension on `ParameterizedType` (default `UNSPECIFIED`, default
  interface methods so other impls stay source-compatible). `ParameterizedTypeImpl` gets a 6-arg
  canonical ctor (the 5-arg delegates → all existing call sites unchanged), folds `nullable` into
  `equals` **but not `hashCode`** (equal→same-hash contract only; keeps hash-ordered collections across
  the analyzer byte-stable). `fullyQualifiedName()` stays nullability-free (semantic key). The bridge
  tags `T?` as `NULLABLE` and boxes nullable primitives via `ensureBoxed`; non-null stays UNSPECIFIED so
  it remains equal to predefined/Java types. Verified no regression in prepwork/inspection-openjdk/
  java-openjdk/cst-impl. (Pre-existing failures in modification-analyzer/-link are unrelated.)
- **M2b variance — DONE.** Added `Variance {INVARIANT, COVARIANT, CONTRAVARIANT}` to `cst-api` and a
  `variance()` accessor on `TypeParameter` (+ `Builder.setVariance`), default `INVARIANT` (Java
  unaffected — Java variance is use-site via wildcards). Stored in the `TypeParameterInspection` layer
  (impl): field + 3-arg ctor + builder setter. The bridge converts a class's declaration-site type
  parameters and maps Kotlin `out`/`in` → `COVARIANT`/`CONTRAVARIANT`. Verified green:
  cst-impl/prepwork/inspection-openjdk/java-openjdk/cst-print.
- **Generics & class-type resolution** (`List<String>`, user types, type-parameter *bounds*, using `T`
  in signatures) still fall back to Object — needs a CompiledTypesManager (M5).

**M3 — Member bodies.** *(First increment DONE.)* Statements & expressions; per the assessment these
mostly *shoehorn* onto existing nodes.
- **DONE:** body structure (block body `{ … }` and expression body `= expr` → `return`/expression
  statement, Unit-aware); `return` statements; literal constants (Int/Boolean/String/null) resolved via
  the session's `evaluate()`. Unsupported expressions become a labelled `newEmptyExpression`
  placeholder (`k2-unsupported-expr:…`) so partial bodies never crash. No CST API change.
- **References — DONE (first expression increment).** `convertExpression` now threads the enclosing
  method and handles `this` (→ `VariableExpression(This)`) and bare name references: to a **parameter**
  (`VariableExpression(ParameterInfo)`) or, failing that, a **field/property** of the enclosing type
  (`VariableExpression(FieldReference)`, params shadow fields). Needed reordering members so properties
  (→ backing fields) exist before method bodies convert, and committing params before the body so
  `method.parameters()` is available. Verified: `fun id(p:Int)=p` → param; `= this` → This; `=v` → field.
- **Operators — DONE.** `a + b` etc. → CST `BinaryOperator` whose operator is the corresponding
  **`Runtime` operator method** (`plusOperatorInt`, `lessOperatorInt`, `andOperatorBool`,
  `plusOperatorString`, …), mirroring java-openjdk's selection. Only built-in operators on
  primitive/String operands are emitted; overloaded operators and Kotlin `==` on objects (`.equals()`)
  fall back to a method-call placeholder.
- **Method calls & qualified access — DONE.** `f(...)` (implicit `this`) and `obj.f(...)` → CST
  `MethodCall` (callee resolved by name + arity on the receiver/enclosing type — works for source &
  loaded-library types); `obj.x` → `VariableExpression(FieldReference scope=obj)`. *Gaps:* inherited-only
  callees and overload ambiguity (same arity) aren't resolved; extension/infix/operator-overload calls,
  named/default args not handled.
- **Assignments & local `val`/`var` — DONE.** A block body now threads a `locals` scope: a local
  `val`/`var` becomes a `LocalVariableCreation` (name, type from the symbol, converted initializer) and
  is added to the scope; references resolve **local → parameter → field**. `x = e` becomes an
  `Assignment` (target a `VariableExpression`; non-assignable targets → placeholder). Verified: a body
  with `val step = 1; count = count + step; return count` produces the local, a field assignment, and the
  local resolving inside the `+` operator. *Gaps:* augmented assignments (`+=`), destructuring, nested
  block scoping (flat map for now).
- **Control flow — DONE (if/while).** `if`/`else` as a statement → `IfElseStatement`; `while` →
  `WhileStatement`; `if` as an expression → `InlineConditional`. A shared `convertBlock` converts a
  branch/body (a `{…}` block or a single statement) into a CST `Block`, with a child `locals` scope per
  block. Verified: `if (n>0){…}else{…}`, `while (i>0){…}`, `= if (n>0) 1 else 0`.
- **`for` loops — DONE.** `for (x in iterable) { … }` → `ForEachStatement` (initializer = the loop
  variable as a `LocalVariableCreation`, expression = the iterable, block = the body); the loop variable
  is added to the body's `locals` scope. Verified: `for (x in items) { total = total + x }` with `x`
  resolving to the loop variable in the body.
- **`when` — DONE (statement, subject form).** `when (subject) { v -> …; a, b -> …; else -> … }` →
  `SwitchStatementNewStyle`: selector = the subject, one `SwitchEntry` per arm (the arm's
  `when`-condition expressions are the case labels — comma-lists give multiple; `else` is a single
  `EmptyExpression`), the body a block. Subject-less `when { … }` uses a `true` placeholder selector.
  Verified: 3 arms incl. a `1, 2 ->` comma arm and `else`. *Gaps:* `is T`/`in range` conditions skipped;
  `when` as an **expression** (→ `SwitchExpression`) still a placeholder.
- **Residue batch — DONE.** `do-while` → `DoStatement`; `break`/`continue` (incl. `@label`) →
  `Break/ContinueStatement`; augmented assignment `+=`/`-=`/`*=`/`/=`/`%=` → `Assignment` with the
  `assign*OperatorInt` method (`setAssignmentOperator`); string templates `"… $x ${e} …"` → folded
  `StringConcat` of literal/escape/interpolation parts. Verified.
- **`when` as expression — DONE.** `= when (n) { … }` → `SwitchExpression` (shared entry-building with the
  statement form; selector, entries, result type). Verified: 3-arm grade function.
- **Lambdas — DONE.** `{ x -> … }` → CST `Lambda`: a synthetic anonymous type (`newAnonymousType`)
  implementing the lambda's functional-interface type, with a single `invoke` SAM method carrying the
  parameters + concrete return type + converted body, wired via `addInterfaceImplemented` /
  `setEnclosingMethod` / `setSingleAbstractMethod`. The three `ParameterizedType`s (functional interface,
  return type, parameter types) come from the resolved `KaFunctionType`. The last body expression becomes
  the implicit return; lambda params resolve via the SAM, outer locals are captured. Trailing-lambda call
  syntax (`list.map { … }`) is wired into `convertCall` (lambda arguments appended). Implicit `it`
  materialised for single-param function types. Verified: `fun makeInc(): (Int)->Int = { x -> x + 1 }`.
  The lambda body is converted in the **enclosing method's context** with the lambda's own parameters
  added to scope, so it captures outer locals, parameters and fields (verified: `{ x -> x * factor }`
  resolves both the lambda param `x` and the captured enclosing param `factor`). *Gaps:* SAM hard-coded
  to `invoke` (Java SAM-conversion targets not handled); `this`/labelled-`this` in receiver lambdas.
- **`when` `is`/`in` conditions — DONE (Java-pattern-compatible).** Per the modern-Java pattern-switch
  model, `is T` is a **type pattern** carried on `SwitchEntry.patternVariable()` (a `RecordPattern` whose
  `localVariable` is typed `T`), with empty `conditions` — *not* an `InstanceOf` condition — so it matches
  how the analyzer reads pattern switches (cf. java-openjdk `doSwitchEntries`). Value arms (`v ->`) are
  case-label conditions; `in range` → a `contains` call condition (`!in` negated via
  `newUnaryOperator`+`logicalNotOperatorBool`). Verified: `when (o) { is Int -> …; is String -> … }` →
  per-arm `patternVariable` typed Int/String. *Gaps:* negated `!is` (not a pattern) dropped; Kotlin
  smartcast binds the subject (the pattern's bound variable is synthetic); `in 0..10` needs `..` rangeTo.
- **Inherited-callee resolution — DONE.** `convertCall` resolves the callee by name + arity on the
  receiver/enclosing type **or its supertypes** (`findMethod` walks parentClass + interfaces, with a
  visited guard). `convertMembers` now applies the hierarchy **before** converting method bodies, so
  `parentClass` is available during resolution. Verified: `class Sub : Base()` calling inherited
  `greet()` resolves to `Base.greet`. *Gaps:* overload disambiguation (still first-by-arity); supertype
  methods must already be converted (declaration/file order); `Object`'s methods aren't on the predefined
  type, so inherited `toString`/`equals` still don't resolve.
- **Access via `computeAccess()` — DONE.** `Access` is the *eventual/computed* accessibility (distinct
  from the declared modifier — a `public` member in a `private` class is eventually restricted). The
  front-end now sets the **visibility modifier** (type modifier on the type; method modifier on
  methods/accessors/constructors; field modifier on backing fields) and calls `computeAccess()` — the
  same canonical path java-openjdk uses — instead of hand-mapping visibility → `Access`. Ordering:
  `convertMembers`/`loadLibraryType` compute the **type** access right after `applyHierarchy`, before any
  member's `computeAccess()` (which reads `owner().access()`). Verified: an abstract interface method is
  `public`; a `private` method is `private`. NB the CST combines with the owner only for **fields** (not
  methods) at inspection time — the full eventual restriction for methods is the analyzer's job.
- **`internal` — DONE (shared CST + front-end).** Added a real **`internal` modifier** to
  `TypeModifier`/`MethodModifier`/`FieldModifier` (+ enum impls, `KeywordImpl.INTERNAL`, `Factory`
  `{type,method,field}ModifierInternal()`) AND a real **`INTERNAL` value to `Access`** (for later code
  analysis): `AccessEnum` is now `PRIVATE(0),PACKAGE(1),PROTECTED(2),INTERNAL(3),PUBLIC(4)` — internal
  just below public (combine = most-restrictive still holds). The three `accessFromX` helpers map the
  internal modifier → `INTERNAL`; the three printers' Access→Modifier converters gained an internal
  branch; `FlagHelper` keyword parsing gained `"internal"`. The Kotlin front-end now emits
  `typeModifierInternal`/`methodModifierInternal` for `internal` declarations → eventual access
  `INTERNAL`. Java gates (`cst-impl`, `modification-prepwork`, `inspection-openjdk`, `java-openjdk`) all
  green. Verified: `internal class Mod { internal fun work() }` → both `access() == accessInternal()`.
- **Overload disambiguation — DONE.** `resolveCallee` collects every name+arity candidate across the
  hierarchy (`collectMethods`) and picks by argument type: exact `ParameterizedType` match, then erased
  `TypeInfo` match, else first. Verified: `handle(1)` → the `Int` overload, `handle("a")` → the `String`
  overload. *Gap:* matching is exact/erased, not full assignability (boxing/widening/generics).
- **Operator-function & infix calls — DONE.** When a binary expression isn't a built-in primitive/String
  operator, `operatorFunctionCall` resolves it as a method call on the left operand: overloaded operators
  use the fixed Kotlin name (`+`→`plus`, `-`→`minus`, `*`→`times`, `/`→`div`, `%`→`rem`), a named infix
  (`a foo b`) uses the reference name, both via `resolveCallee`. Verified: `a + b` on `V` → `a.plus(b)`;
  `a upTo b` → `a.upTo(b)`. *Gaps:* comparison/equality operator-functions (`<`→`compareTo`, `==`→`equals`)
  not desugared (they're more than a call); extension-function receivers still unresolved (members only).
- **File facade `<FileName>Kt` — DONE (increment 1: top-level functions).** Following the JVM model
  (top-level declarations compile to a synthetic facade class), the front-end now creates a
  `<FileName>Kt` `TypeInfo` per file that has top-level functions (name = file sans extension, first
  char upper-cased, + `Kt`), registers it in `InfoByFqn` (pass A), and adds its top-level functions as
  **static** methods (`methodTypeStaticMethod` + static modifier; `convertMethod(static=true)`). It is a
  final public class committed in pass B2 and returned among the parsed types. Verified: `Greet.kt` with
  `fun greet(name: String): String` → type `GreetKt` with static `greet`. **Top-level properties — DONE:**
  static backing field + static accessors whose field access is qualified by the facade type
  (`Type.field`, not `this.field`); `convertProperty`/`buildGetter`/`buildSetter`/`assignFieldFromParam`
  took a `static` flag + shared `fieldAccessScope`/`methodType` helpers, `setGetSetField` tagging intact.
  Verified: `val version: Int = 1` → static field `version` + static `getVersion`.
- **Top-level extension functions — DONE (declaration side).** `convertMethod` prepends an extension's
  receiver (`function.receiverParameter`) as a synthetic first parameter `$receiver`, so
  `fun String.tag(suffix: String)` → facade static `tag(String $receiver, String suffix)` — exactly the
  JVM shape (also applies to member extensions). Verified arity/types. *Next:* call-site resolution
  (`"abc".tag(x)` → `ExtKt.tag("abc", x)`, receiver as arg 0) and body `this`/unqualified-receiver-member
  → the receiver param (currently a placeholder). Then `@file:JvmName`.
- **Reusable note:** the facade is the file-level container ONLY; companion objects / named objects map
  to their own JVM types (`Outer$Companion` + `Companion` field, `INSTANCE` singletons) via the same
  *synthesize-and-register* mechanism, not the facade itself.
- **TODO:** `@file:JvmName` facade name; extension-function calls (facade + receiver-as-first-param);
  companion/named objects (own JVM types via the synthesize-and-register pattern); `..` rangeTo (rangeTo
  on the primitive `Int` has no member to resolve); negated `!is`.

**M4 — Kotlin-specific info.** `PropertyInfo`, primary constructors, extension receiver, `suspend`,
`object`/`data`/`companion`, `internal` access, default parameter values — each gated on its CST API
addition from the assessment doc (priority order ranked there).
- **Type structure — DONE.** Source types now get their real **nature** (interface/enum/annotation via
  `classKind`; `object`/`data class` → class for now) and **supertypes** (parent class + interfaces),
  via a shared `applyHierarchy` helper reused by source and library loading. Verified:
  `class Circle : Base(), Shape` → parent `Base`, interface `Shape` (both source types); `interface`/
  `enum class`/`object` natures classify correctly.
- **Constructors — DONE.** Primary and secondary constructors convert to CST constructors via the
  existing `newConstructor`/`addConstructor` API; the body assigns the backing fields for parameters
  that correspond to properties (`this.x = x`), so the field-init/immutability analysis sees them
  initialised (reuses the same `assignFieldFromParam` helper as the setter). Verified:
  `class Point(val x: Int, var name: String)` → constructor(x, name) with a 2-assignment body;
  `class Multi(val a) { constructor() : this(0) }` → 2 constructors.
  - **Delegation (`this(...)`/`super(...)`) — DONE** via `ExplicitConstructorInvocation`. The scan is now
    a 3-pass: A register types, B1 members + constructor *structures* (no body), B2 wire each
    constructor's body (delegation ECI first, then field assignments) and commit — so even `super()` to
    another *source* type resolves (all constructors exist by B2). A primary super-type call
    `class Sub : Base(5)` and a secondary `: this(0)` both become an ECI (`isSuper` set, target resolved
    by arity, args via `convertExpression`). Verified. *Gaps:* target resolved by arity only (not full
    overload resolution); `super()` to a library type whose constructors aren't loaded is skipped;
    implicit `super()` is not emitted.
- **Properties — DONE (harmonized with maddi's getter/setter normalization).** A Kotlin property
  (`val`/`var`, incl. primary-constructor `val x: Int`) becomes a backing `FieldInfo` (private; `val`→
  final) **plus accessor methods whose bodies maddi already recognises**: `getX() { return this.x; }`
  and (for `var`) `setX(v) { this.x = v; }`, JavaBean-named to match the JVM names Kotlin generates. Each
  accessor is tagged via `runtime.setGetSetField` (mirroring `RecordSynthetics.createAccessor`), so the
  analyzer's getter/setter normalisation (`GetSetHelper`) treats Kotlin property access identically to a
  Java field access. Verified: `class Point(val x: Int, var name: String)` → fields x(final)/name,
  accessors getX/getName/setName, each `getSetField().field()` linked to its field. **Computed properties
  — DONE:** a property with `hasBackingField == false` (e.g. `val sum get() = x + y`) becomes just a
  getter with its real (converted) body — no field, no field-tagging (verified: `Point` has x/y but not
  `sum`; `getSum()` returns `x + y`). *Gaps:* custom accessor bodies on backing-field properties,
  delegates, and the `PropertyInfo`-node model deferred.
- **Visibility & modifiers — DONE.** `accessFor`/`addMethodModifiers` map Kotlin visibility
  (`private`/`protected`/`public`; `internal`→public for now — no CST `Access` yet) and modality
  (`abstract`/`final`; `open`→none) onto types (Access + type modifier) and methods (Access + method
  modifiers), at all four sites (source/library × type/method). Verified: `abstract class`/`abstract fun`
  carry abstract, `private fun` is private, Kotlin-default classes are `final`, `open` classes are not.

**M5 — Type manager, driver & integration.** Mirrors openjdk's split: `ClassSymbolScanner` is the
*internal* type manager; `CompiledTypesManager` is the *external* one (shared across source sets, used by
maddi clients — **its API must not change**, it's already used by the maddi & openjdk parsers).
- **M5a internal source-type manager — DONE.** `KotlinScan` now does a two-pass scan: pass A creates +
  registers every type of the current compilation (with its type parameters) in an internal
  `sourceTypes` map by FQN; pass B converts members so references resolve. `mapType` now resolves
  references to sibling source types (with generic arguments) and bare type parameters (`T`), in
  addition to the builtins. No CST API change.
- **M5b external/library types — DONE (first increment).** Reframed after studying `ClassSymbolScanner`:
  the **Analysis API is the loader of jars+JDK** (as javac is for openjdk), so we don't bolt on maddi's
  bytecode manager — we convert the resolved library symbols ourselves. `KotlinSymbolScanner` (the
  `ClassSymbolScanner` analogue) is a receptacle seeded with `runtime.predefinedObjects()` plus lazy
  shell-creation, keyed by **JVM FQN**. Decision: Kotlin's *mapped* types (`JavaToKotlinClassMap`) →
  JVM FQN (`List`→`java.util.List`, `String`→`java.lang.String`, `Any`→`Object`), so the Kotlin
  front-end emits the *same* `TypeInfo`s as the Java parsers (uniform CST; the shared
  `CompiledTypesManager` — whose API we must not change — and analyzer `java.*` knowledge apply
  directly). `Int` boxes to `Integer` in generic-argument position. Read-only vs mutable collapses to
  one `java.util.List`; the read-only signal, if wanted, belongs in the `Info` **property map** as a
  modification status, not a divergent type identity.
  *Simplifications to revisit:* shells carry identity + arity but not a full hierarchy (parent=Object,
  no interfaces/members); nested types (`Map.Entry`), raw types, and faithful type-parameter names
  deferred.
- **M5c driver — IN PROGRESS.** Foundation done: `KotlinScan.convert(ktFiles)` does a **global** two-pass
  (register all files' types, then convert members) so references resolve **across files**, sharing one
  `InfoByFqn`; `parse(Map<name,content>)` drives a multi-file session. The session is now owned at the
  top so a driver can wire a classpath before calling `convert`.
  **Real classpath — DONE.** `buildSession` now gives the standalone session the running JVM's JDK
  (`buildKtSdkModule` + `addBinaryRootsFromJdkHome`) and this process's classpath
  (`buildKtLibraryModule` over `java.class.path`) as dependencies, so any JDK/library type referenced in
  Kotlin source resolves to a real symbol (verified: a non-builtin `java.util.UUID` resolves to FQN
  `java.util.UUID`). Library types still get a shell `TypeInfo` (correct identity + arity), not yet a
  full hierarchy/members.
  **Deepen library shells — DONE (hierarchy).** `loadLibraryType` converts a non-mapped library/JDK type
  from its real `KaNamedClassSymbol` (the analogue of `ClassSymbolScanner.loadType` LAZILY): type nature,
  type-parameter arity, and the supertype hierarchy (parent class + interfaces), registered before its
  supertypes load so cycles terminate. Verified: `java.util.UUID` resolves with parent `Object` and
  interfaces `java.io.Serializable` + `java.lang.Comparable`. Mapped types (`List`, `String`, …) stay
  shells — their Kotlin-builtin symbol is not the Java view, so deepening them needs the actual Java
  symbol (later).
  **Members — DONE (methods).** `loadLibraryType` now also loads method signatures (params + return
  type, no body + `methodMissingMethodBody` marker) on a *directly-referenced* library type. A one-level
  reentrancy guard (`loadingMembers`) keeps the cascade bounded: types named only by member signatures
  load hierarchy-only. Verified: `java.util.UUID` has `toString`/`compareTo`, and
  `getMostSignificantBits(): long` resolves.
  *Known gaps:* inherited/method-level **type parameters** resolve to Object (e.g. `compareTo`'s `T`
  from `Comparable<T>`); static-ness/modality/visibility not yet mapped (all public); fields &
  constructors not loaded; type-parameter **bounds** deferred; a type loaded hierarchy-only (as a
  supertype or member-signature type) won't gain members if later referenced directly (needs a
  `COMPLETE`-style re-load, à la `ClassSymbolScanner`).
  **Module extracted — DONE.** `maddi-inspection-kotlin` (the analogue of `maddi-inspection-openjdk`)
  holds `KotlinInspector` (an `InputConfiguration`-driven driver that owns the shared `InfoByFqn` and
  drives `KotlinScan`) and `KotlinCompiledTypesManager` — a receptacle that is a **view over the shared
  `InfoByFqn`** (its `get` delegates to `getType`). Verified: after `parse`, the manager's
  `get("java.util.UUID")` returns the **same instance** (`assertSame`) the scan produced. `KotlinScan`
  now takes the `InfoByFqn` as a constructor arg so the driver owns it.
  *Remaining:* deepen **mapped** types from their Java symbol; member modifiers (static/modality/
  visibility), fields, constructors; multi-source-set ordering; classpath from `InputConfiguration`
  (currently the running process's); the full `JavaInspector` surface; round-trip print via `print2`.

### Multi-source-set & mixed Kotlin/Java modules (design note)

How the openjdk parser handles source sets (`JavaInspectorImpl`):
- Source sets form a **dependency DAG** (`SourceSet.dependencies()`); `computeScanOrder()` topologically
  linearizes it and **rejects cycles** (`"Cycles in the source set graph"`). External libraries are
  excluded from the ordering and loaded on demand by `ClassSymbolScanner`.
- Sets are scanned **one at a time, in dependency order**; a dependent set sees its dependencies' types
  already inspected. `dependencies()` order = class-by-class priority.
- **One shared `InfoByFqn` registry** is threaded through every source set, so single-instance per
  (FQN, source set) holds across the whole project (see [[info-identity-equality]]).

Consequences for Kotlin:
- **Between source sets it is strictly one-directional** (no cycles) — Java→Kotlin *or* Kotlin→Java.
- **A real mixed module is mutually referential** (Java refs Kotlin and vice versa) — that is *not* two
  cyclic source sets. The compiler resolves it **asymmetrically, Kotlin-first**: `kotlinc` compiles
  `.kt` while *reading* `.java` source (Kotlin resolves against Java source); then `javac` compiles
  `.java` against Kotlin **bytecode**. So a mixed module is one unit, processed Kotlin-first, with both
  parsers sharing one type registry.
- **The shared registry is `InfoByFqn`.** Decision: promote `InfoByFqn` out of `maddi-java-openjdk` into
  `maddi-inspection-resource` so both the openjdk parser and the Kotlin front-end use the same instance.
  The Kotlin loader must `put`/`getType` against this shared `InfoByFqn` (keyed by JVM FQN, per M5b)
  rather than mint private shells — otherwise a mixed program gets two `java.util.List` instances and
  `==` breaks. This is the concrete mechanism that makes M5b's JVM-FQN mapping pay off.

## 6. First test to write (M1 acceptance)

```kotlin
// input
class Foo { fun bar(): Int = 1 }
```
```java
// assertion (sketch, mirrors java-openjdk TestClass1)
TypeInfo foo = kotlinScan.parse("Foo", source);
assertEquals("Foo", foo.simpleName());
assertEquals(1, foo.methods().size());
MethodInfo bar = foo.methods().getFirst();
assertEquals("bar", bar.name());
assertEquals("int", bar.returnType().fullyQualifiedName()); // or boxed Integer — TBD in M2
```

## 7. Risks

- **R1 (high) → RETIRED in M0.** Standalone Analysis API artifact availability resolved; jars download.
- **R2 (med):** Kotlin plugin / JDK target alignment — daemon runs JDK 26; Kotlin 2.4 caps target at
  JVM 25 (a warning, builds fine). No toolchain forced.
- **R3 (med):** Kotlin↔Java builtin type mapping (`Int`↔`Integer`, `Unit`↔`void`, `Nothing`,
  flexible/platform types from Java interop).
- **R4 (low, deferred):** Bazel parity (D4).

## 8. M0 results (verified 2026-06-27)

- **Latest stable Kotlin = 2.4.0.**
- **Hosting:** the standalone Analysis API ships as `*-for-ide` artifacts on
  `https://packages.jetbrains.team/maven/p/ij/intellij-dependencies`, **not** Maven Central. Added to
  `settings.gradle.kts` centrally (the project uses `FAIL_ON_PROJECT_REPOS`).
- **Verified artifact set @ 2.4.0** (transitivity disabled; the FIR impl was renamed):
  `analysis-api-for-ide`, `analysis-api-k2-for-ide` *(was `high-level-api-fir-for-ide`)*,
  `analysis-api-impl-base-for-ide`, `low-level-api-fir-for-ide`,
  `analysis-api-platform-interface-for-ide`, `symbol-light-classes-for-ide`,
  `analysis-api-standalone-for-ide`, plus `kotlin-compiler` (Maven Central).
- **Standalone-runtime extras** (the `*-for-ide` jars are stripped; discovered by walking
  `NoClassDefFoundError`): `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0`,
  `com.github.ben-manes.caffeine:caffeine:3.1.8`, and the **IntelliJ-patched coroutines**
  `org.jetbrains.intellij.deps.kotlinx:kotlinx-coroutines-core:1.10.2-intellij-1` (provides
  `kotlinx.coroutines.internal.intellij.IntellijCoroutines`, absent upstream; cf. detekt#9176). The
  upstream coroutines that `kotlin-compiler` drags in is substituted out via
  `resolutionStrategy.dependencySubstitution` so only one set of `kotlinx.coroutines.*` is present.
  That was the **complete** chain — no fastutil/trove needed.
- **API surface:** the spike compiled against `buildStandaloneAnalysisAPISession`,
  `buildKtModuleProvider`/`buildKtSourceModule`, `analyze {}`, `KaClassSymbol`,
  `KaNamedFunctionSymbol` — all present. `buildKtModuleProvider` is a *member* of the session builder
  (not a top-level import).
- **Session bootstraps, runs, and resolves — green.** `StandaloneApiSpike.resolvesSimpleClass()` passes:
  it builds a standalone session over an in-memory `class Foo { fun bar(): Int = 1 }`, calls
  `analyze {}`, and asserts the resolved class symbol (`Foo`), its function (`bar`), and return type
  (`kotlin/Int`). End-to-end proof that the K2 Analysis API is usable as the CST source of truth.

---

*Next action: M2 — real signatures & the type layer. Parameters (`ParameterInfo`), proper return/param
type conversion with the Kotlin↔Java builtin mapping (`kotlin/Int`→int, `kotlin/Unit`→void,
`kotlin/String`→String), generics, then the assessment's type-layer gaps (nullability, variance).*
