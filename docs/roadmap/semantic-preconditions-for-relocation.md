# Semantic preconditions for state-relocating refactorings

**Written 2026-09-25, revised the same day with evidence from real runs.** Expands
[`modification-link-applications.md`](modification-link-applications.md) §3.1 from a candidate into a
design. It is still a *proposal*: nothing here is built, and nothing here asks the engine to conclude
anything different. §1 and §2 are survey results and reproductions, measured against the trees on the date
above. §3 onwards is design.

> **PARKED 2026-09-25 by the maintainer.** Reasons and state at parking:
> - **Frequency:** rare per move, and concentrated. About 470 moved non-constant statics across six campaigns
>   produced 8 defects, all in OpenSearch and all one pattern (appending into an existing holder). The monitor
>   hazard had 0 realized cases, and unqualified `wait`/`notify` appear in about 0.04% to 2% of source files.
>   The other hazards had 0 cases.
> - **Cheaper cover already exists:** the optional test gate (atomic writes, 2026-09-23) plus the relevant
>   tests would have caught all 8 (3 test classes, about 20 s).
> - **If resumed:** contain it in one place instead of spreading it over the DSL methods. The editor's per-file
>   before/after oracle hook (where the conformance oracle runs) fits both evidenced checks, §3.1 and §3.2, as a
>   second oracle, with refusal through the existing rollback. The link-based checks (§3.4, §3.5) do not fit
>   that hook and have no evidence; drop them. The OpenSearch defects of §2.1/§2.2 were fixed in the campaign
>   tree.

**The short version.**

- The downstream levers that relocate state validate only name and accessibility facts. Their one
  behavioural warning is syntactic, and scripts routinely override warnings.
- **This has already broken real code**, in levers other than split class (§2):
  - Move static members, on a modularization run of OpenSearch, committed three moves that break at class
    initialisation. One makes a settings holder fail to load, which makes `ClusterSettings` unloadable.
    Two more make index-setting defaults resolve to `null` or `0`. Every change compiled, and every build
    gate passed. All three were reproduced by loading the classes.
  - Method object, run on Pulsar's `RateLimiter.acquire`, produces code that **deadlocks**. The rewrite
    was reproduced with the lever itself and then executed.
- The first version of this document ranked the hazards wrongly. The two that broke real code are:
  - a static initialiser reading a static declared later in the same class;
  - a `wait()`/`notifyAll()` left bound to the wrong object.

  Neither needs link analysis. Both are decidable from the CST and the call graph (§3.1, §3.2).
  Link analysis earns its place on the tail (§3.4, §3.5), for which no real instance has been found yet.
- Two of the levers already handle `this` identity well (§2.6). The claim that `this` escapes unchecked
  holds for split class only.
- Every check must return **found with a witness, clean, or not checked** (§5). A degraded or partial
  analysis must never read as clean.

---

## 1. What the levers check today

Survey of the downstream refactoring stack, 2026-09-25. Levers are named by their verb.

| lever | relocates | validation today | reads maddi analysis? |
|---|---|---|---|
| move type / move types to sub-project | a type (no state) | name collisions, accessibility, package cycles, resources | no |
| extract companion | static members to a new or existing holder | collisions, retained↔moved references, accessibility, a `static{}` block that must travel; **warns** on a moved static field with a non-constant initialiser | no |
| move static members | static members to an existing type | extract companion's checks, plus batch interference | no |
| split class | instance fields and methods, into parts | **none**; failures surface as exceptions | no |
| attach state | instance fields and methods, into an attachment object | retained↔moved references, **any use of `this`**, implicit calls on retained members, dynamic type tests, collisions | no |
| method object | a method's body and locals, into a new object | unreachable private members, uneditable signatures; **rewrites bare `this`** to a back-reference | no |
| receiver to parameter | an instance method, made static | rewrites `this` to the new parameter | no |
| pull up member | a declaration (no state) | supertype editable, not already declared | no |

Four facts shape the design:

1. **Split class has no validation step.** It is the lever with the most freedom to relocate state and the
   only one with no place to put a finding.
2. **The partitioner never sees a field.** Clustering runs over methods only. A method-method edge weighs
   *presence* of a call-graph edge (a fixed 500), plus a user prior, plus a name-similarity prior. Fields are
   attached after clustering, and a service-typed field is **duplicated** into each part that needs it.
3. **Warnings do not bind.** 307 modularization scripts pass `ignoreWarnings=True`, and two verbs document
   that it is "normally required". Only a `conflict` stops a write.
4. **The one behavioural check is syntactic.** Extract companion and move static members warn when a moved
   static field has a non-constant initialiser. The warning cannot tell a harmless `new HashMap<>()` from a
   read-before-init, so it fires on both, and it is overridden on both.

---

## 2. Evidence: what happened on real code

Each case says how it was established: a committed change from a recorded run that was executed, the
lever run on a reduction of real code and the result executed, or code reading only.

### 2.1 Move static members, OpenSearch: a settings holder that cannot initialise

*Committed by a modularization run; reproduced by loading the classes of that build.*

Two moves into an existing holder, `IndicesSettingKeys`, a day apart. The first moved
`INDICES_REQUEST_CACHE_CLEANUP_INTERVAL_SETTING` from `IndicesRequestCache`. Its initialiser reads a fallback
setting that still lived in `IndicesService`:

```java
// IndicesSettingKeys, after the first move -- correct: the fallback is in another class,
// and the JVM initialises that class on demand
public static final Setting<TimeValue> INDICES_REQUEST_CACHE_CLEANUP_INTERVAL_SETTING = Setting.positiveTimeSetting(
    IndicesRequestCache.INDICES_REQUEST_CACHE_CLEANUP_INTERVAL_SETTING_KEY,
    IndicesService.INDICES_CACHE_CLEAN_INTERVAL_SETTING,
    Setting.Property.NodeScope);
```

The second move brought that fallback into the same holder. The lever did two things. It **appended** the
moved field at the end of the class, and it **retargeted** the earlier read to the new home:

```java
// IndicesSettingKeys, after the second move
public static final Setting<TimeValue> INDICES_REQUEST_CACHE_CLEANUP_INTERVAL_SETTING = Setting.positiveTimeSetting(  // line 313
    IndicesSettingKeys.INDICES_REQUEST_CACHE_CLEANUP_INTERVAL_SETTING_KEY,
    IndicesSettingKeys.INDICES_CACHE_CLEAN_INTERVAL_SETTING,    // read here ...
    Setting.Property.NodeScope);
...
public static final Setting<TimeValue> INDICES_CACHE_CLEAN_INTERVAL_SETTING = Setting.positiveTimeSetting(  // ... declared at line 567
    IndicesService.INDICES_CACHE_CLEANUP_INTERVAL_SETTING_KEY,
    TimeValue.timeValueMinutes(1),
    Setting.Property.NodeScope);
```

javac forbids a forward reference only by *simple* name (JLS 8.3.3). The qualified name compiles. At class
initialisation, line 313 reads a field that is still `null`, and `positiveTimeSetting` dereferences it.
Loading the classes of the committed build:

```
IndicesSettingKeys -> ExceptionInInitializerError / root NullPointerException @ IndicesSettingKeys.<clinit>(IndicesSettingKeys.java:313)
IndicesSettingKeys -> NoClassDefFoundError: Could not initialize class IndicesSettingKeys
ClusterSettings    -> NoClassDefFoundError: Could not initialize class IndicesSettingKeys
```

No particular access order is needed. `ClusterSettings` is built at every node start, so the node cannot
start. The run's gates compile and run selected tasks, and no test touched the holder, so nothing reported it.

### 2.2 Move static members, OpenSearch: defaults that read as `null` and `0`

*Committed by the same campaign; reproduced by reading each setting's default in that build.*

The same shape, in `IndexSettingKeys`, in a way that does not stop the class from loading. Settings moved from
`TieredMergePolicyProvider`, `LogByteSizeMergePolicyProvider` and `StarTreeIndexSettings` read defaults that
were appended later in the holder:

```java
public static final Setting<ByteSizeValue> INDEX_MERGE_POLICY_FLOOR_SEGMENT_SETTING = Setting.byteSizeSetting(  // line 1124
    "index.merge.policy.floor_segment",
    IndexSettingKeys.DEFAULT_FLOOR_SEGMENT,                 // declared at line 1725, still null here
    ...);
public static final Setting<Integer> STAR_TREE_MAX_DIMENSIONS_SETTING = Setting.intSetting(               // line 1027
    "index.composite_index.star_tree.field.max_dimensions",
    IndexSettingKeys.STAR_TREE_MAX_DIMENSIONS_DEFAULT,      // declared at line 1707, not final, still 0 here
    2, ...);
```

Resolving the defaults with empty settings:

```
INDEX_MERGE_POLICY_FLOOR_SEGMENT_SETTING -> NullPointerException: Cannot invoke "ByteSizeValue.getBytes()" because "value" is null
STAR_TREE_MAX_DIMENSIONS_SETTING         -> IllegalArgumentException: Failed to parse value [0] ... must be >= 2
```

The first fails for every index that does not set a floor segment explicitly. The same happens for
max-merged-segment and the three log-byte-size settings. The second rejects every star-tree mapping that does
not set its limit.

**Prevalence.** About 410 moved non-constant static fields were inspected on this campaign. Eight change
behaviour, and all eight are this one mechanism. In every case the destination was an **existing** class
whose earlier fields read the moved ones. Moves into fresh holders were clean. The campaign's own design notes
had named the hazard, "appended statics would read as null (textual clinit order)", and then restricted it to
one destination.

### 2.3 Method object, Pulsar `RateLimiter.acquire`: a deadlock

*Lever run on a reduction of the real method (same fields, same monitor protocol); both versions executed.*

```java
// pulsar-common RateLimiter, lines 140-159 and 270-283
public synchronized void acquire(long acquirePermit) throws InterruptedException {
    ...
    do {
        canAcquire = acquirePermit < 0 || acquiredPermits < this.permits;
        if (!canAcquire) {
            wait();                          // releases RateLimiter's monitor until renew() runs
        } else {
            acquiredPermits += acquirePermit;
        }
    } while (!canAcquire);
}
synchronized void renew() { ...; notifyAll(); }   // scheduled at a fixed rate
```

The lever's output, verbatim:

```java
class Acquire {
    private final RateLimiter rateLimiter;
    public Acquire(RateLimiter rateLimiter) { this.rateLimiter = rateLimiter; }
    public synchronized void acquire(long acquirePermit) throws InterruptedException {
        ...
            if (!canAcquire) {
                wait();                      // now waits on the Acquire object
            } else {
                rateLimiter.acquiredPermits += acquirePermit;
        ...
    }
}
// and in RateLimiter:
public synchronized void acquire(long acquirePermit) throws InterruptedException {
    new Acquire(this).acquire(acquirePermit);     // still holds RateLimiter's monitor
}
```

The lever rewrites every *bare* `this` to the back-reference. It deliberately leaves the methods of
`java.lang.Object` alone, so that `toString()` keeps resolving. `wait()` is one of those methods. After the
move it waits on the method object and releases only that monitor, while the delegate keeps holding
`RateLimiter`'s. `renew()` can never enter, and even if it did, its `notifyAll()` targets the wrong object.
The same harness was run on both versions: take the only permit, start a second `acquire`, then call
`renew()` from another thread.

```
before: client: second acquire returned / renewer: renew() returned / RESULT: permit renewed, client proceeded
after:  RESULT: client still blocked after 2s, renewer blocked too (BLOCKED)
```

It compiles cleanly, and no current check refuses it. Cassandra's `TraceState.waitActivity(long)` has the same
shape with `wait(timeout)` (code reading only). There the result is not a deadlock: the waiter sleeps its
full timeout while holding `TraceState`'s monitor, and `notifyActivity()` blocks for that long.

### 2.4 Two milder re-timings

- **Move static members, Apache Ignite** (committed): `TTL_BATCH_SIZE = IgniteSystemProperties.getInteger(...)`
  moved from `GridCacheUtils`, which initialises at node start, to a utility class that initialises at the first
  cache operation. A system property set programmatically in between is now honoured where it used to be ignored.
  This needs an unusual order to observe, and a `-D` flag behaves the same before and after.
- **Extract companion, Elasticsearch** (committed on a working branch): `LOGGER = LogManager.getLogger(QueryPhase.class)`
  moved with its class literal rewritten to the companion's class. The logger category changed name, so an
  operator's `logger.org.elasticsearch.search.query.QueryPhase: trace` now matches nothing. This is diagnostic
  only, but it is a silent change, and no warning covers it.

### 2.5 Code reading only: locks that move

- **Receiver to parameter** inserts `static` after the existing modifiers and does not refuse `synchronized`.
  `synchronized void m()` becomes `synchronized static void m(A p)`, so the lock moves from the receiver to the
  class. No recorded run hit a synchronized target.
- **Extract companion / move static members** move a `static synchronized` method verbatim. Its lock moves from
  the origin's class object to the destination's. No instance was found.

### 2.6 Counter-evidence: what the levers already get right

- **Attach state refuses any use of `this` in moved code**, and any implicit call on a retained member. That
  covers `return this`, `register(this)`, `synchronized (this)`, `wait()`, `getClass()` and `hashCode()`. On a
  Cassandra run it refused real code for exactly this reason:
  `notifyPreChanges(new SchemaTransformationResult(prev, this, ksDiff))`. Moved and retained members cannot share
  fields in either direction, which rules out §3.5's "same state, two locks" through fields. The residue is `==`:
  the lever's scan covers casts, `instanceof`, class literals and method references, but not identity comparison.
- **Method object rewrites every bare `this`** to a back-reference, including `synchronized (this)`,
  `this.wait()` and `this == o`. This was learnt the hard way. On fernflower, `stat.setParent(this)` passed the
  method object and was "silently wrong" until the rewrite existed. Pulsar's `LocalRunner.start` writes
  `this.wait()` and is handled correctly. `RateLimiter` writes `wait()` and deadlocks. The hazard is in the
  qualifier, not in the design.
- **No recorded incident** of a split monitor or a diverging duplicated field was found in any campaign log.

---

## 3. The hazards, ranked by evidence

### 3.1 Static read-before-init in the destination (move static members, extract companion)

*Evidence: §2.1, §2.2 (eight real defects). Decidable from the CST and the call graph. No link analysis.*

When statics land in an **existing** class, the class's initialisation order is its textual order. A static
initialiser (a field initialiser or a `static{}` block) that runs *before* a moved field's new position, and
reads that field, sees its default value (`null`, `0`, `false`). The read may be direct, by any
qualification, or through a static method the initialiser calls. javac catches only the direct read by simple
name.

**Precondition.** After placement, no static initialiser of the destination reads a static declared later in
the destination, unless that static is a constant variable (JLS 4.12.4, which is inlined). This covers:

- direct reads under any qualification;
- reads through static methods of the destination, via the call graph (bounded depth; *not checked* past the
  bound);
- reads among the moved members themselves, since their relative order is the lever's choice.

**Witness:** the reading initialiser and its line, the field read, and its declaration line. For example,
`IndicesSettingKeys:313 reads INDICES_CACHE_CLEAN_INTERVAL_SETTING (declared :567)`.

The lever can often avoid the conflict rather than report it. It can insert the moved field *before* its first
reader, provided the moved field's own initialiser reads nothing declared after that point. The precondition
is what makes that placement decision checkable.

### 3.2 Monitor methods bound to the wrong object (method object, split promote, receiver to parameter, extract companion)

*Evidence: §2.3 (reproduced deadlock). §2.5 (code reading). Decidable from the CST.*

Moved code that calls `wait`, `notify` or `notifyAll` with an **implicit** receiver, or a moved `synchronized`
method or block whose monitor changes object, changes which lock is held or signalled. The lever has a choice
per case. It can qualify the call with the back-reference, move the synchronisation, or refuse.

**Precondition** (method object, split promote): moved code contains no unqualified `wait`/`notify`/`notifyAll`.
If the original method is `synchronized`, the moved copy must not be. The delegate already holds the original's
monitor, and a second monitor on a fresh object is at best useless.

**Precondition** (receiver to parameter, extract companion): a `synchronized` or `static synchronized` method is
refused, or its synchronisation is rewritten to `synchronized (p)` / `synchronized (Origin.class)` explicitly.

**Witness:** the call or modifier, and the object it bound to before and after.

`getClass()`, and `hashCode()`/`equals()`/`toString()` when not overridden, fall under the same "implicit
`Object` method" rule. They do not deadlock, but they change value. They belong to the same check.

### 3.3 Re-timed static initialisation with effects (move static members, extract companion)

*Evidence: §2.4, mild. Needs `STATIC_SIDE_EFFECTS_METHOD`, which is not usable yet (§4).*

An initialiser whose effect depends on *when* it runs changes behaviour when it moves to a class that
initialises at a different moment. Such effects include: a side effect on other state, a read of a system
property or mutable static, a logger named after its class, or an exception thrown at class init (which now
poisons a different class). This is the hazard the existing warning was written for. Its real instances are
mild, and the warning cannot separate them from the pure majority.

**Rule for maddi data.** A found side effect may *escalate* the warning to a conflict. The absence of one may
never *suppress* the warning (§4). The logger case is syntactic: a class literal rewritten inside a moved
initialiser. It can be reported without any analysis.

### 3.4 `this` identity (split class; residue elsewhere)

*Evidence: handled well by attach state and method object (§2.6); split class unchecked.*

A method that moves to a new object gets a new `this`. Behaviour changes when the old `this` was observable:

- returned, as a fluent `return this`;
- stored somewhere that outlives the call, such as a listener registration or a map;
- used as a monitor (§3.2);
- compared by identity.

Split class rewrites bare `this` only in argument, return and assignment positions. It leaves
`synchronized (this)` and `==` alone, and it copies `synchronized` modifiers. The residue in the other levers
is the implicit `Object` methods (§3.2) and attach state's missing `==` case.

The part that needs **link analysis** is `this` escaping *through a callee*. That means a moved method passes
`this`'s fields to a callee that stores them, which a syntactic `this` scan cannot see. The data is available:
`METHOD_LINKS`, with `ofParameters()` naming `this`. No real instance has been found yet.

### 3.5 A monitor split across parts, and a duplicated field (split class)

*Evidence: none found; the mechanisms are real. Needs link analysis.*

- **Monitor split.** Two `synchronized` methods that end up in different parts no longer exclude each other.
  If they modify the same object, directly or through aliasing fields (`this.a.§m ≡ this.b.§m`), a race appears.
  The `§m` identity is what a field-level check misses.
- **Duplicated field.** A service-typed field copied into two parts is safe when it is effectively final and
  assigned once in the constructor. Both copies then hold the same reference, and content changes are shared.
  It diverges when the field is ever reassigned.

The first version of this document put these at the top. On the evidence they belong at the bottom. They stay
here because split class has no validation step at all (§1), so nothing else would catch them.

### 3.6 A correction to parent §3.1's example

Parent §3.1 says two fields aliasing one mutable object, split across parts, become "two objects each holding
half" of an invariant. A split moves **references, not objects**, so aliasing survives it. Aliasing matters
only as the thing that makes §3.5's monitor split reach further than the moved field. Otherwise it is a
design-quality concern for the partitioner (§6), not a precondition.

### 3.7 Where no semantic precondition applies

Move type, move types to a sub-project and pull up member relocate declarations, not state. The criterion is
this: *a lever needs a semantic precondition exactly when it changes which object holds a piece of state,
which monitor guards it, or the order and moment in which it is initialised.*

---

## 4. What maddi provides today, fact by fact

| needed fact | for | where it lives | status |
|---|---|---|---|
| static initialisers, their order, their reads | §3.1 | CST: field `initializer()`, `<static_N>` methods (`isStaticInitializer()`), source positions | available |
| which static methods an initialiser reaches | §3.1 | call graph (`ComputeCallGraph`) | available |
| constant variables | §3.1 | CST (initialiser a constant expression, field `final`) | available (the downstream stack has its own `isConstantVariable`) |
| implicit-receiver calls to `Object` monitor methods; `synchronized` | §3.2 | CST: `MethodCall` with implicit `this`; `MethodInfo.isSynchronized()`; `SynchronizedStatement` | available |
| a static initialiser has a side effect | §3.3 | `STATIC_SIDE_EFFECTS_METHOD` on `<static_N>` methods | **gated** (env `SSE`, off by default); **field initialisers not examined**; constructor calls not examined; a bodyless callee with no contract counts as FALSE |
| a method lets `this` escape through its return or a callee | §3.4 | `METHOD_LINKS` → `ofReturnValue()`, `ofParameters()`; `FLUENT_METHOD`, `IDENTITY_METHOD` | available |
| what a method modifies, including via callees | §3.5 | `METHOD_LINKS` → `modified()` | available |
| two fields share a mutable object | §3.5 | field `LINKS`, `§m ≡` between fields | available; no tested direct `this.a ≡ this.b` entry; recoverable through a shared constructor parameter |
| object-graph overlap `∩ ≤ ≥` | — | closure | **off** under `LinkComputer.Options.PRODUCTION`; do not depend on it |
| a field is effectively final | §3.5 | `FINAL_FIELD` | available |
| a summary is degraded | all link-based checks | `DEGRADED_ANALYSIS_METHOD` | available |

Two consequences:

- **The evidenced checks (§3.1, §3.2) need the CST and the call graph, not link analysis.** maddi supplies both,
  and both are available whenever the project is parsed, so these checks can run on every write.
- **`STATIC_SIDE_EFFECTS_METHOD` may only be read as TRUE.** FALSE also means "no body and no contract", so
  reading it as "pure" repeats the degraded-summary mistake (§5).

---

## 5. Three values, and why presence is not enough

Parent §2 ranks applications by presence versus absence, and calls presence-based checks sound under
saturation. That holds for saturation. **It does not hold for degradation.** A degraded method gets a shallow
summary that reports *fewer* links than the truth. A presence check over it finds nothing, and for a
precondition "found nothing" is the green light.

The same holds for the structural checks, in their own form. A call-graph walk cut off at a depth bound, or a
callee without source, has not established that no read happens.

So every check returns one of three values:

- **Found** carries a witness. The lever files a conflict.
- **Clean** is returned only when everything involved was fully analysed: source, not degraded, within bounds,
  analysis enabled.
- **NotChecked** names what was missing.

`NotChecked` has to survive `ignoreWarnings=True`, or it has no force. The `precondition` severity already exists
in the response contract and is not part of the ignored warning stream.

Proposed shape:

```java
sealed interface Finding {
    record Found(String witness, List<Info> involved) implements Finding { }
    record Clean() implements Finding { }
    record NotChecked(String why, List<Info> involved) implements Finding { }
}
Finding staticReadBeforeInit(TypeInfo destination, List<Info> placedInOrder);  // §3.1
Finding implicitMonitorBinding(List<MethodInfo> moved);                         // §3.2
Finding thisEscapes(MethodInfo moved);                                          // §3.4 (link-based)
Finding monitorOverlap(MethodInfo m, MethodInfo n);                             // §3.5 (link-based)
Finding duplicationSafe(FieldInfo f);                                           // §3.5
```

The link-based checks belong in maddi (`maddi-modification-analyzer`). Their rules are engine knowledge that a
consumer would get subtly wrong:

- `§m` is the modification component;
- the `☷` variant of `≡` excepts some methods;
- `RedundantLinks` folds `≈ ≺ ≻` into `≈`.

The structural checks can live on either side. They are listed with the others so that all of them share one
`Finding` type and one policy for `NotChecked`.

---

## 6. The objective-function side

Parent §3.1 names two gaps: the checks, and the objective function that chooses a split. Preconditions refuse a
bad split after it is chosen. The partitioner clusters methods only, so aliasing would have to enter it as
**method-method weight**: two methods that modify content in the same `§m` class get an edge, as if one called
the other. That keeps invariant-maintaining methods together, and makes §3.5's monitor split rarer. It is a
scoring change downstream that needs its own measurement. It comes after the preconditions, because a
partitioner nudge prevents nothing.

For statics, the equivalent of an objective is **placement**. §3.1 is best discharged by inserting moved fields
before their first reader, not by refusing.

---

## 7. Order of work

| | item | lever(s) | data | evidence |
|---|---|---|---|---|
| 1 | `staticReadBeforeInit` (§3.1), plus reader-aware placement | move static members, extract companion | CST, call graph | eight committed defects (§2.1, §2.2) |
| 2 | `implicitMonitorBinding` (§3.2) | method object, split promote, receiver to parameter, extract companion | CST | reproduced deadlock (§2.3) |
| 3 | a validation step in split class, carrying §3.2 and a syntactic `this` check | split class | CST | code reading (§3.4) |
| 4 | `duplicationSafe`, `thisEscapes`, `monitorOverlap` (§3.4, §3.5) | split class, attach state | `METHOD_LINKS`, field `LINKS`, `FINAL_FIELD` | mechanisms only |
| 5 | method-method weight from shared `§m` (§6) | split class partitioner | as 4 | design quality |
| 6 | static side effects, escalate-only (§3.3) | move static members, extract companion | `STATIC_SIDE_EFFECTS_METHOD` (needs: on by default; field initialisers and constructor calls covered) | mild (§2.4) |

Items 1 and 2 would have prevented every behaviour change found on real code, and neither waits for engine work.

---

## 8. How to know it works

- **The real cases are the first fixtures.** They are the OpenSearch holder shape (§2.1), the default-read shape
  (§2.2) and the `RateLimiter` monitor shape (§2.3). Each has a harmless twin that must come back `Clean`: a
  qualified read of a field declared *earlier*, a constant-variable read, `this.wait()` in method object.
- **Replay the OpenSearch campaign's static moves** through item 1. It must flag the eight known defects, and the
  number of other flags across the ~410 non-constant moves is the false-positive rate. A check that fires on
  most real moves will be overridden like the warning is, so that rate is a design constraint.
- **A degraded twin of each link-based fixture** (forced with a low `maddi.workCeiling`) must come back
  `NotChecked`, never `Clean`.
- **maddi's own CST** as a no-corpus proving ground for §3.4: it is builder-heavy, with fluent returns throughout.

## 9. Open questions

- Should `NotChecked` block when the involved code is on the moved side, and not block when it is only on the
  retained side? The asymmetry is plausible but unargued.
- §3.1's call-graph walk needs a depth bound. What bound keeps it cheap on holders with hundreds of settings,
  while still reaching the builder methods that dereference a default?
- `assigned()` is not populated across `this(...)`/`super(...)`, so §3.5's "assigned once in the constructor" needs
  its own walk for delegating constructors.
- Field `LINKS` has no tested direct field-field entry. Before §3.5 relies on it, a fixture should establish
  whether `this.a.§m ≡ this.b.§m` is written for `this.a = p; this.b = p;`, or must be composed from the
  constructor's parameter links.
