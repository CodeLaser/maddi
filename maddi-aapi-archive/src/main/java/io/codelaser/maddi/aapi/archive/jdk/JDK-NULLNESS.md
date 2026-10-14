# JDK hints: nullability from JSpecify's annotated JDK

Status: 2026-10-13. Covers the `@Nullable` and `@NotNull` in the hint files next to this report (`Java*.java`, except
AWT and Swing), and their compiled results in `src/main/resources/.../analyzedPackageFiles/jdk`.

## Where it comes from

[JSpecify's JDK](https://github.com/jspecify/jdk) is OpenJDK with JSpecify nullness annotations: classes are
`@NullMarked`, so an unannotated reference is non-null, a type variable follows its type argument, and `@Nullable`
marks what may be null. It began as a fork of the Checker Framework's annotated JDK and now merges from OpenJDK.
The hints take the facts it states (which positions of the public API accept or return null), not its code; the
generator reads commit `6e433342` (2026-10-13). The upstream repository is GPLv2 with the Classpath Exception, as
OpenJDK is.

Until this change the JDK hints said `@Nullable` for a hand-made list (the former `LibraryNullness` stopgap:
`Map.get`, `Queue.poll`, `comparator()`, `System.getProperty`, ...) and `@NotNull` here and there; every other
reference position was UNSPECIFIED.

## What the nullability pass does with it

- a `@Nullable` return seeds null: a value from that call is nullable unless a test removes it. A missing one is
  unsafe: the value is read as non-null.
- a `@NotNull` parameter makes the passed value non-null after the call, and tells the pass that the JDK never
  calls an override back with null there. An UNSPECIFIED parameter of an overridden library method makes the
  override's parameter nullable for Kotlin (`libraryUnhintedParameter`), which was the main source of noise.
- a `@Nullable` parameter accepts null; for an override it is nullable.
- a non-null return seeds nothing, so the generator does not write it.

## What the generator takes

`corpus/scripts/jspecify-jdk/jspecify-jdk-nullness.py` (its javadoc has the rules). For every public or protected
member that JSpecify declares, of a JDK type in a package with a hint file:

| JSpecify | hint |
|---|---|
| `@Nullable` return | `@Nullable`, unless the null can only come from an argument or a function (below) |
| `@Nullable` parameter | `@Nullable` |
| `@Nullable` type argument, wildcard bound or array elements | `nullableArguments` (below) |
| unannotated reference parameter in `@NullMarked` code | `@NotNull` |
| type-variable parameter or return | nothing: the instantiation decides |
| non-null return | nothing |

A member the hints did not shadow gets a declaration from `javap` of the recorded JDK release; a type they did not
shadow gets a shadow class, marked `(generated)`. equals, hashCode, toString and Object's final methods are skipped.

**Returns null only through an argument.** `Optional.orElse(other)`, `orElseGet(supplier)`, `Map.getOrDefault`,
`compute`, `computeIfAbsent`, `merge`, `Class.cast`, `Objects.requireNonNullElse(Get)`, and the two-argument
defaults `System.getProperty(key, def)`, `Properties.getProperty(key, def)`, `Integer.getInteger`, `Long.getLong`,
`Objects.toString(o, nullDefault)`, `Charset.forName(name, fallback)`, `ObjectInputStream.GetField.get(name, def)`:
JSpecify must say `@Nullable`, but the pass follows the argument per call (the "gap 9" tests in
`TestNullabilityPass`), which a hint would override. They stay unhinted.

**Null only if a null was written.** `ObjectInputStream.readObject()`/`readUnshared()` and `ObjectInput.readObject()`:
JSpecify says `@Nullable`, but a class's `readObject` reads back what its `writeObject` wrote. As `@Nullable` they
seeded null in guava's serialization proxies, which reached `ForwardingMap.delegate()` and from there about 170
non-null positions of the oracle on a stale two-source-set guava configuration (1 on the current one). They stay
unhinted.

Together these are 39 members (`TRUSTED_RETURNS`, `PASS_NAMES`, `PASS_SIGS` in the script).

**Kept ours, where they disagree.** `Objects.requireNonNull` and `requireNonNullElse`'s default: JSpecify says the
argument may be null (that is what the method checks); ours says non-null, which the pass reads as "non-null after
the call". `OutputStreamWriter.append(CharSequence)`: JSpecify's non-null is its `@NullMarked` default, but
`Writer.append` appends `"null"`; ours says `@Nullable`.

**Ours corrected.** Our `@NotNull` was wrong on `Writer.append(CharSequence)` (four overloads), `Predicate.isEqual`,
`Stream.ofNullable`, and `FilterOutputStream(OutputStream)`; they follow JSpecify now. `EnumMap`'s shadow gained the
bound `K extends Enum<K>`.

**JSpecify corrected.** `Matcher.group()` is `group(0)`, the whole match: never null after a successful match, and
it throws `IllegalStateException` without one. JSpecify's `@Nullable` on it (not on `MatchResult.group()`) made every
`if (m.find()) ... m.group()` a null seed: the diagnose session's sample on timefold, 2026-10-13. Only `group(int)` and
`group(String)` answer null, for an optional group that did not participate; they stay `@Nullable`.

## One level down: elements and type arguments

JSpecify annotates 170 members of java.base and java.logging below the top level, always one level deep: an array's
elements, a type argument, or a wildcard's bound. The generator writes them as `nullableArguments` on the position's
annotation, the positions that may hold null (0 is an array's elements):

| JSpecify | hint |
|---|---|
| `@Nullable Object[] toArray()` | `@NotNull(nullableArguments = 0) Object[] toArray()` |
| `@Nullable Object @Nullable ... values` (`Objects.hash`) | `@Nullable(nullableArguments = 0)` |
| `BiFunction<? super K, ? super @Nullable V, ? extends @Nullable V>` (`Map.compute`) | `@NotNull(nullableArguments = {1, 2})` |
| `Function<? super T, ? extends @Nullable U>` (`Optional.map`) | `@NotNull(nullableArguments = 1)` |

188 positions (126 parameters, 62 returns). A non-null return with a nested fact gets `@NotNull`, which it otherwise
would not (the attribute needs a host). An existing plain annotation of the same state is upgraded in place; one with
other attributes (`content = true`) is left and listed. The override rule of the shallow analyzer passes the argument
states on when the override declares the same shape, so guava's and Eclipse Collections' `toArray()` overrides compile
`N(Q)`.

What the nullability pass does with them, and the measurement: maddi-mod `docs/design/nullability.md`, entry
"2026-10-13 — JDK hints one level down".

## Numbers

Compiled JDK results, nullability entries before → after:

| | before | after |
|---|---:|---:|
| `@Nullable` returns | 161 | 596 |
| `@Nullable` parameters | 173 | 906 |
| non-null parameters (primitives included) | 2,689 | 7,048 |

The other properties of the existing entries did not change (a diff of every key but the nullability ones); the
7,738 entries the new shadow members add carry the shallow defaults the analyzer computed for them before.
Guava's and Eclipse Collections' results changed in their inherited nullability only.

## Measurements (2026-10-13)

`TestNullabilityOracleGuava` on laser1 (the current six-source-set guava configuration), the JDK results before and
after, all positions (top level, type arguments, elements):

| policy | agree | unsafe | noise | undecided |
|---|---|---|---|---|
| `NULL_MARKED` | 17,919 → 17,905 | 368 → 357 | 1,706 → 1,738 | 304 → 297 |
| `OPEN_VISIBILITY` | 13,622 → 13,766 | 222 → 217 | 1,709 → 1,741 | 4,744 → 4,573 |
| `OPEN` | 15,067 → 15,269 | 226 → 221 | 1,709 → 1,741 | 3,295 → 3,066 |

The unsafe positions that go are values from JDK calls that may return null. In the open-world policies, the
`@NotNull` parameters decide override parameters that were undecided. The new noise all comes from returns the JDK
documents as nullable where guava's context guarantees a value: `Class.getResource` (9), `Path.getFileName` (7, null
for a root), `ParameterizedType.getOwnerType` (6, null for a top-level type), `Method.invoke` (4, null for a void
method), `File.getParentFile` (2), `ThreadFactory.newThread`, `ClassLoader.getParent`, `Class.getEnumConstants`,
`Class.getGenericSuperclass`. The contract is right, and the pass cannot see the context.

The J2K ratchets are unchanged: fernflower 199/199 files compile, 264 translated tests pass; langchain4j 344/346,
1,106. The aapi-parser, prepwork and analyzer suites pass; six tests that pinned an earlier JDK nullability were
updated.

## Regenerating

1. A sparse, blob-less clone of `https://github.com/jspecify/jdk` with `src/java.base`, `src/java.logging` and
   `src/java.net.http` (`git sparse-checkout set --no-cone 'src/java.base/share/classes/java/' ...`).
2. From the maddi root, on the recorded JDK release: `python3 corpus/scripts/jspecify-jdk/jspecify-jdk-nullness.py
   <clone>` reports; `--write` changes the hint files. A second run changes nothing.
3. `:maddi-aapi-parser:compileAnalysisHints` (maddi-mod), then the suites and the measurements above.
