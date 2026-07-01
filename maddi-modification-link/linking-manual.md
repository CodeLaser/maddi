# Linking — a working manual

A practical, session-to-session reference for the `maddi-modification-link` module: what linking is *for*, how the
pieces fit, the notation, the central class (`LinkMethodCall`), how to probe/test it, and the known rough edges.

It complements two existing docs — read them alongside this one:
- **`README.md`** (this module): the authoritative list of link natures and the fixpoint operator table.
- **`src/main/java/org/e2immu/analyzer/modification/link/vf/virtual-fields.md`**: the `§` virtual-field / hidden-content model.
- Vocabulary of *mutable / immutable / independent / accessible vs hidden content* lives in the `road-to-immutability`
  asciidoc (authoritative for terminology).

---

## 1. What linking is for

Linking answers one question at every expression: **how do the variables here relate to one another, with a view to
proving or disproving modification and immutability?** A link is a triple `(from, linkNature, to)`. The set of links
for a method is summarized as its `MethodLinkedVariables` (mlv): how the **return value**, **parameters**, and **object
(receiver)** relate, plus which of them are **modified**.

Downstream, the analyzer uses this to decide `@NotModified`/`@Modified`, `@Independent`, and immutability. Linking
itself does not decide those — it produces the relational substrate.

---

## 2. The pipeline

```
parse (openjdk or maddi parser) → CST
   └─ PrepAnalyzer (maddi-modification-prepwork)   per method: VariableData (assignments, reads, scopes),
                                                    and the method's own MethodLinkedVariables (mlv) "summary"
        └─ LinkComputer / LinkComputerImpl (this module)
             per method body, statement by statement (doBlock/doStatement),
             expression by expression (ExpressionVisitor),
             applying each callee's mlv at the call site (LinkMethodCall)
             → the method's METHOD_LINKS (its own mlv), reached by a fixpoint
```

- **PrepAnalyzer** is upstream; it must run before LinkComputer (`new PrepAnalyzer(runtime, opts).doPrimaryType(t)`).
- **LinkComputerImpl.doMethod(mi)** computes one method's mlv; **doPrimaryType** does a whole type (with recursion via
  `recurseMethod`, guarded by `RecursionPrevention`). Results are stored in `analysis()` under `METHOD_LINKS`.
- A method's own mlv is reached by a **fixpoint propagation** over the link graph (`LinkGraph`,
  `TestFixedpointPropagationAlgorithm`), combining link natures with a binary operator (see README table).

---

## 3. Data model (key types)

- **`Result`** (`impl/Result.java`) — the output of visiting an expression. Fields:
  - `links` (`Links`) — the *primary* link set (its `primary()` is the expression's main variable).
  - `extra` (`LinkedVariables`) — links about *other* variables touched (a `Map<Variable,Links>`).
  - `modified` (`Map<Variable,Set<MethodInfo>>`) — variables modified, and by which method call.
  - plus `evaluated` (the possibly-rewritten expression), `casts`, `writeMethodCalls`, `variablesRepresentingConstants`.
  - `merge`, `addModified`, `copyLinksToExtra`, `moveLinksToExtra`, `expandFunctionalInterfaceVariables`.
- **`MethodLinkedVariables`** (mlv) — a method's summary: `ofReturnValue()` (a `Links`, primary is a `ReturnVariable`),
  `ofParameters()` (a `List<Links>`, one per formal parameter), `modified()` (a `Set<Variable>`), `translate(tm)`.
  `sortedModifiedString()` gives a stable string of the modified set (used heavily in tests).
- **`Links` / `Link` / `LinkNature`** — a `Link` is `(from, linkNature, to)`; `Links` is a set with a `primary()`.
- **Variable kinds you will meet** (important for reading link output):
  - `ParameterInfo` (`0:box`, `1:x` — the index prefix), `FieldReference` (`box.t`, `this.myBox`), `This`,
    `DependentVariable` (array access `arr[0]`, `boxes[0]`), `ReturnVariable`.
  - synthetic/marker: `IntermediateVariable` (return-value/parameter placeholders `$__rv`, `$_..`),
    `FunctionalInterfaceVariable` (`Λ`/`$_fi`), `AppliedFunctionalInterfaceVariable` (`$_afi`),
    `ObjectCreationVariable`, `MarkerVariable`, and the `§`-virtual-field variables (see virtual-fields.md).

---

## 4. Notation cheat-sheet (how to read link strings)

The `toString()` of an mlv is `[<param links>] --> <return links>`, e.g. `[0:box*.t←1:x*, 1:x*→0:box*.t] --> -`.

- **Link natures** (full list + combination table in `README.md`):
  identity `≡`; element level `∈` (is element of) / `∋` (contains as member) / `⊆` / `⊇` / `~` (shares elements);
  field level `≺` (is field of) / `≻` / `≈`; object graph `≤` / `≥` / `∩`; decoration (functional interfaces)
  `↗` (is decorated with) / `↖` (contains decoration); assignment `←` (assigned from) / `→` (assigned to).
- **`*`** after a variable = it is (or may be) **modified** at that point (e.g. `box*`, `this.myBox*.t`).
- **`0:` / `1:`** = parameter index; **`.t` / `.a`** = a field; **`[0]`** = array access; **`§…`** = a virtual field
  / hidden content (e.g. `§xs`, `§es`, `§m`); **`∈∈`** = element-of-element (nested).
- **`$__rv`** return-value intermediate, **`$_ce`** constructed-element (record/for-each pattern binding),
  **`$_fi`** functional-interface variable, **`$_afi`** applied-functional-interface, **`Λ`** functional-interface marker.
- `[-]` = one parameter with no links; `[-, -]` = two; `--> -` = no return links.

---

## 5. `LinkMethodCall` — the central class

> The single most important class. It applies a callee's `mlv` to a concrete call site, translating the callee's
> formal `this` / parameters / return into the actual object / arguments / return, and emits the links.

Entry points (called from `ExpressionVisitor`):
- **`constructorCall(cc, object, params, mlv)`** — `new T(...)`. Copies arg links into `extra`, computes
  param→object links (`parametersToObject`), optionally records an `ObjectCreationVariable`.
- **`methodCall(methodInfo, concreteReturnType, object, paramsIn, mlv)`** — the core. Order:
  1. `expandParams` — expand functional-interface arguments *when the callee will actually invoke them* (external
     library, or a param link/return targets an `AppliedFunctionalInterfaceVariable`).
  2. `copyParamsIntoExtra` — non-functional-interface arg links flow into `extra`.
  3. **return value** links, three cases:
     - `samOfFunctionalInterface` — the call *is* the SAM of a standard functional interface (`apply`, `accept`…):
       decorate the object with an `AppliedFunctionalInterfaceVariable`.
     - `objectToReturnValue` — normal: walk `mlv.ofReturnValue()`, translate `this`+params→actuals; functional-interface
       arms go through `translateHandleFunctional`.
     - empty return.
  4. **object vs static**:
     - receiver present → `parametersToObject` (how arguments flow into the object; consumer-style FI params handled
       via `LinkFunctionalInterface`).
     - static (no receiver) → `linksBetweenParameters` (argument↔argument, incl. varargs fan-out via `downgrade`)
       and `appliedFunctionalInterfaces`.

Helper map: `objectToReturnValue`, `parametersToObject`, `linksBetweenParameters` / `addCrosslink` / `downgrade` /
`varargsLinkNature`, `samOfFunctionalInterface` / `appliedFunctionalInterfaces` / `translateHandleFunctional`,
`addThisHierarchyToObjectPrimaryToTmBuilder` (maps `this` and supertypes' `this` to the object primary — this is how
inherited-method receivers and `this.field` chains resolve).

**Behavioral spec:** `src/test/java/.../impl/TestLinkMethodCall.java` is the readable, parse-once
(`@TestInstance(PER_CLASS)`) specification-by-example. It uses self-contained holders (`Box<T>` one field,
`Pair<A,B>` two, `Triple<P,Q,R>` three, `SubBox extends Box`) so each test isolates one path. Start there to see the
exact links for: constructors (arg→field, multi-arg, generic factory, from-accessors); accessors/receivers
(field→return, static-receiver, inherited, `this.field` chain, nested `Box<Box<X>>`, array-index, 2-D array,
multi-field selectivity, identity pass-through, fluent/`this`-returning); mutations (arg→field with modification,
returns-`this`, no-arg modifier, holder-in, two-arg setter, field/array/nested receivers); static param↔param
(incl. varargs fan-out).

**Hardening already applied** (behavior-preserving unless noted):
- Removed a dead duplicated-`if` branch in `parametersToObject` (the from-side param→object case is deliberately
  *not* handled — activating a plausible variant breaks `TestForEachMethodReference`/`TestStreamBasics`).
- `downgrade` reworked: the plural-name logic (`vs`→`v`, with its `endsWith("s")` assert) now applies **only to
  multi-dimensional** varargs; a single dimension (`T... vs`) or an already-indexed element (`vs[0]`, a
  `DependentVariable`) resolves to the actual argument. Previously it *crashed* on single-dim `LocalVariable` varargs
  and on `vs[0]`.

---

## 6. Actual linking behavior — worked examples

This is the section to internalise. All examples are real, current output (`method :: <mlv> MOD[<modified>]`); they
come in **Box/List pairs** so you can see the *same relationship* expressed over a source type and over a JDK type.

### 6.1 Two field models: concrete `.t` vs virtual `§xs`

A link names a *place inside* a variable. How that place is named depends on whether the analyzer can see the type's
fields:

- **Source / seen types (`Box<T>` with `private T t`)** — the field is visible, so links name it directly: `box.t`,
  `pair.a`, `bb.t.t`. `Box` holds **one** value.
- **Abstract / shallow-analyzed types (`List<X>`, most JDK types)** — no field is visible. Their contents are a
  **virtual field** (hidden content), written `§xs` for a collection's elements (`§es`, `§kvs`, `§m` for the
  modification marker — see `vf/virtual-fields.md`). `List` holds **many** elements, so `§xs` is *multi-element*.

That single/multi distinction drives which link nature appears. `Box` (one value) tends to use assignment/identity
(`←`, `→`); `List` (many elements) uses element relations (`∈`, `∋`, `~`, `⊆`). Otherwise the shapes are identical.

Reading aids: `*` after a variable = *modified there*; a link and its **reverse** usually both appear
(`a←b` alongside `b→a`, `x∈y` alongside `y∋x`) — that is expected, not duplication.

### 6.2 Accessor — the container's content flows to the return value

```
Box:   X read(Box<X> box) { return box.get(); }     ->  [-] --> read←0:box.t
List:  X get(List<X> list){ return list.get(0); }   ->  [-] --> get∈0:list.§xs
```
`box.get()` returns *the* field value → `read ← box.t` (return **assigned from** the single field). `list.get(0)`
returns *one of many* elements → `get ∈ list.§xs` (return **is an element of** the hidden content). No `*`: neither
call modifies anything.

### 6.3 Mutator — an argument flows into the container, which is modified

```
Box:   void set(Box<X> box, X x){ box.set(x); }      ->  [0:box*.t←1:x*, 1:x*→0:box*.t] --> -   MOD[…:0:box, …:1:x]
List:  void add(List<X> list, X x){ list.add(x); }   ->  [0:list*.§xs∋1:x, 1:x∈0:list*.§xs] --> -  MOD[…:0:list]
```
Both put the argument *inside* the receiver and mark the receiver modified (`box*`, `list*`). Box: `box.t ← x` (the
one field is assigned from x). List: `list.§xs ∋ x` (x becomes a member of the hidden content). Each shows the
reciprocal (`x→box.t`, `x∈list.§xs`). Note Box also marks `x` modified (its content is now reachable-and-mutable via
`box`); the shallow List summary marks only `list`.

### 6.4 Construction / copy — the new object shares the source's content

```
Box:   Box<X> make(X x){ return new Box<>(x); }            ->  [-] --> make.t←0:x
Box:   Box<X> makeFromGet(Box<X> b){return new Box<>(b.get());} -> [-] --> makeFromGet.t←0:b.t
List:  List<X> copy(List<X> l){ return new ArrayList<>(l); }->  [-] --> copy.§xs⊆0:list.§xs
```
The constructor's return value is the new object; its content is fed from the arguments. `make.t ← x` (field from
argument). `copy.§xs ⊆ list.§xs` (the copy's elements are a **subset of** the source's — a copy *shares* elements,
it does not clone them deeply).

### 6.5 Static, argument-to-argument (`linksBetweenParameters`)

```
Box:   static <T> void transfer(Box<T> from, Box<T> to){ to.set(from.get()); }
                 ->  [0:from.t*→1:to*.t, 1:to*.t←0:from.t*] --> -   MOD[…:0:from, …:1:to, from.t]
List:  boolean addAll(List<X> dst, List<X> src){ return dst.addAll(src); }
                 ->  [0:dst*.§xs~1:src.§xs, 1:src.§xs~0:dst*.§xs] --> -   MOD[…:0:dst]
```
No receiver — links run **between the arguments**. Box: `from.t → to.t` (the value moves from one holder's field to
the other's). List: `dst.§xs ~ src.§xs` (`~` = *shares elements*; `addAll` makes dst's and src's element sets
overlap). `to`/`dst` are modified.

### 6.6 Pure and modify-only calls

```
List:  int size(List<X> list){ return list.size(); }   ->  [-] --> -    MOD[]       (pure: no links, no modification)
List:  void clear(List<X> list){ list.clear(); }        ->  [-] --> -    MOD[…:0:list] (modifies, but relates nothing)
Box:   void clear(Box<X> box){ box.clear(); }           ->  [-] --> -    MOD[…:0:box]
```
`size` is the pure case — nothing flows, nothing changes. `clear` changes the receiver but creates no relationships
(the content is dropped, not connected).

### 6.7 Nesting, fields-of-`this`, arrays — the same rules, deeper names

The place-names simply get longer; the natures are unchanged.

```
nested:      X nestedRead(Box<Box<X>> bb){ return bb.get().get(); }   ->  [-] --> nestedRead←0:bb.t.t
this.field:  X readField(){ return this.myBox.get(); }                ->  [] --> readField←this.myBox.t
array elt:   X arrayGet(Box<X>[] boxes){ return boxes[0].get(); }     ->  [-] --> arrayGet←0:boxes[0].t
array elt:   Box<X> arrayElement(Box<X>[] b){ return b[0]; }          ->  [-] --> arrayElement←0:boxes[0],arrayElement∈0:boxes
2-D return:  X grid2dElement(X[][] g){ return g[0][0]; }              ->  … g2dElement∈0:grid[0], g2dElement∈∈0:grid
```
`bb.t.t` = field of a field; `this.myBox.t` = field of `this`'s field; `boxes[0].t` = field of an array element;
`∈∈` = element-of-element (returning `grid[0][0]` is an element of a row which is an element of the grid).

> These are all pinned in `impl/TestLinkMethodCall.java` (Box/Pair/Triple/arrays) and, for JDK collections and
> language constructs, in `impl/TestList.java`, `impl/TestMap.java`, `impl/TestStream.java`,
> `typelink/TestLanguageConstructs.java`. When in doubt about a shape, add a probe (see §8) and read the actual output.

---

## 7. Functional interfaces — TODO

Deep dive deferred. Lambdas / method references become a `FunctionalInterfaceVariable`, applied via
`LinkFunctionalInterface` / `LinkAppliedFunctionalInterface`; there is a known open gap around hidden-content
propagation through `map`/`apply` of an element-extracting function. **To be documented in a dedicated pass.**

---

## 8. Probing & testing workflow

The reliable loop is **probe → read actual → assert** (link output is exact-string-pinned; do not hand-derive `$_ce`
counters — they depend on parse order, so isolate inputs or read them from a probe run).

Minimal harness (link module `CommonTest` gives `javaInspector`, `runtime`):
```java
TypeInfo c = javaInspector.parse("a.b.C", INPUT);
new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(c);
MethodLinkedVariables mlv = new LinkComputerImpl(javaInspector)
        .doMethod(c.methodStream().filter(m -> m.name().equals("m")).findFirst().orElseThrow());
System.out.println(mlv + "  MOD[" + mlv.sortedModifiedString() + "]");
```
- For a constructor, fall back to `c.constructors().getFirst()` (constructors aren't in `methodStream()`).
- Parse the **same fqn only once** per test method (a second `parse("a.b.C", …)` throws "already committed" — use
  distinct type names or parse once and cache, e.g. `@TestInstance(PER_CLASS)` as `TestLinkMethodCall` does).
- Use **self-contained holder types** (`Box`/`Pair`/`Triple`) instead of JDK collections when you want to control the
  callee `mlv` precisely and keep output stable (fields `t`/`a`/`b` instead of `§xs`).
- Language-construct coverage (var, ternary, switch/patterns, try-with-resources, wildcards, arrays, enum, inner
  classes, …) lives in `src/test/java/.../typelink/TestLanguageConstructs.java`.

**Baseline** (as of this writing): the full link suite is green except two long-standing failures —
`TestList.shallow (multiplicity 1 instead of 2)` and `TestShallowFunctional.find (return type)`. Treat any *third*
failure as a regression. Watch for a flaky `IllegalStateException: tree.starImportScope is null` — that is the openjdk
parser's javac concurrency issue, mitigated by `-XDuseUnsharedTable=true` (see the memory of the same name); it is not
a linking bug.

---

## 9. Known gaps / quirks (running list)

- **FI element-extraction gap** — §7 (open, deferred).
- **`parametersToObject` from-side** — the param→object case where the object part is a link's `from` is not handled
  (dead branch removed + documented in code). Revisit if a legitimate case surfaces.
- **`TestList.shallow` / `TestShallowFunctional`** — pre-existing baseline failures in the HC/multiplicity area.
- Historically fixed here (don't re-break): for-each over a *bare-type-parameter array* now links (`m∈0:arr`) while
  varargs-of-collections keeps the iterable path; try-with-resources now propagates resource modification (the
  resource declaration is processed as an LVC — `LinkComputerImpl.subBlocksForLinking`).

---

## 10. File map (module `maddi-modification-link`, package `…link.impl`)

- `LinkComputerImpl` — driver: `doMethod`/`doPrimaryType`, `doBlock`/`doStatement`, for-each/for handling,
  `subBlocksForLinking` (try-with-resources), fixpoint.
- `ExpressionVisitor` — per-expression visit; dispatches method/constructor calls to `LinkMethodCall`, handles
  lambdas/method refs (`methodReference`), conditionals, switch expressions, array access (`DependentVariable`).
- `LinkMethodCall` — §5.
- `LinkFunctionalInterface`, `LinkAppliedFunctionalInterface` — FI application (§7, TODO).
- `LinkGraph`, `LinkNatureImpl`, `Result`, `LinkedVariablesImpl`, `Links`/`Link` (in prepwork `…variable.impl`).
- `vf/VirtualFieldComputer`, `vf/VirtualFields` (+ `vf/virtual-fields.md`).
- `impl/localvar/…` — the synthetic variable kinds (`FunctionalInterfaceVariable`, `IntermediateVariable`, …).
- Tests: `impl/TestLinkMethodCall` (the spec), `typelink/TestLanguageConstructs`, plus many `impl/Test*` for
  specific shapes (List, Map, Stream, Record, GetSet, Varargs, Dependent, Cast, InstanceOf, …).

---

*Starting point for a new session: read §5 (`LinkMethodCall`) + §6 (worked examples) + `TestLinkMethodCall`; §7
(functional interfaces) is a TODO to be filled in a dedicated pass. Prove changes with the §8 probe loop and the
two-failure baseline noted in §8.*
