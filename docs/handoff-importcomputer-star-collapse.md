# Handoff: `ImportComputerImpl` star-collapsing changes name resolution

**Written 2026-08-02.** Found on the closed-core corpus via the jfocus dedup intake; the consumer
is fixed downstream (never collapse), this note is the upstream defect record. Status: **open**.

## The defect

`ImportComputerImpl.go` collapses ≥ `minStar` single-type imports of one package into an on-demand
(`.*`) import. Collapsing is **not semantics-preserving**: a star import participates in JLS 6.5.5
name resolution, so it can turn a name that the original file resolved through a single-type import into an
**ambiguity** — javac produces an error symbol and (in the re-parse pipelines) the whole compilation unit is
dropped as "Type X not found".

Trigger, measured at scale: closed-core declares public JDK homonyms —
`com.example.core.general.util.ArrayList` (extends `java.util.ArrayList`),
`com.example.core.general.util.InstantiationException`, `com.example.core.sys.query.Record`. An original
unit importing `java.util.ArrayList` (single-type) plus ≥4 types of `…general.util` (single-type) printed as
`java.util.*` + `com.example.core.general.util.*`, making every bare `ArrayList` ambiguous. **106 of 135
re-parse failures on a 357-type slice were this one shape.**

## Why the existing guard misses it

`conflict(packageWithStar, typesReferenced)` is designed to suppress exactly such stars, but:

1. **Its package listing is the compiled types manager**
   (`JavaInspector.importComputer`: `compiledTypesManager().primaryTypesInPackageEnsureLoaded(…)`). A
   homonym parsed from **source** is missing from that listing — `CompiledTypesManagerImpl.addToTrie`
   removes the compiled entry when the type also arrives as source. So the guard is blind precisely on
   corpora where the colliding type is part of the parsed project.
2. **`java.lang` collisions are invisible.** `allowInImport` keeps java.lang types out of
   `typesReferenced`, so a starred package containing a public `InstantiationException` never registers as
   conflicting with the implicit `java.lang` on-demand import.

## Suggested fix directions (pick one)

- Make `conflict()` consult a union listing (source-parsed + compiled types for the package), and check the
  starred package's public simple names against **java.lang's** as well; or
- retire star-collapsing for machine-consumed prints entirely (the jfocus intake now passes
  `minStar = Integer.MAX_VALUE`, as `IsolateClass` already did — `NEVER_COLLAPSE_TO_STAR`).

A collapse that survives must prove, per bare simple name used in the unit, that resolution is unchanged —
anything less re-opens this by another homonym.

## Reproduction

Downstream: `jfocus-standardize/codelaser-standardize-deduplication` —
`TestIntakeAttrition.jdkHomonymStarCollapse` (fails when the intake collapses, passes with never-collapse).
The shape is trivially portable to a maddi-level test: a source-parsed public `p.ArrayList`, a unit with
`import java.util.ArrayList;` + ≥4 single imports of `p`, print with computed imports, re-parse.
