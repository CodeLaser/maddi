# Performance of the J2K corpus runs, 2026-10-22

Where the time goes when maddi translates a corpus to Kotlin, measured with async-profiler. This is the
baseline for the hash-caching change (§3), and the reference for later ones.

## 1. How it was measured

- Mac Studio, `lab run --host mac --mb 16g`. maddi `fa9d872b7`, maddi-mod `412948e6`.
- `J2K_COMPILE_ONLY=true TESTXMX=12G ./gradlew -q :maddi-run-analysis:slowTest --tests "*TestJavaToKotlin<Corpus>Nullability" --rerun`.
- async-profiler 4.6, through the build's `ASPROF` hook (`maddi-mod/maddi-run-analysis/build.gradle.kts`):
  `ASPROF="start,event=cpu,alloc=2m,lock=10ms,interval=10ms,file=<dir>/<Corpus>-%p.jfr,jfr"`.
  - Use `%p`: a second Gradle test executor starts after the main one, with the same agent options. Without the
    process id in the file name it overwrites the main recording, leaving a 0-second file.
  - Pick the long recording: `jfr summary` shows its duration.
- What the profile covers: the test JVM, which does parse, prep, the modification analysis, the nullability pass and
  the Kotlin printer. kotlinc runs in a child JVM of its own and is not in it.
- *wall* is the whole Gradle invocation, kotlinc included (`date` around it). *JVM* is the recording's duration.
  *CPU-s* is the CPU samples × 10 ms.

## 2. Baseline (before hash caching)

| corpus | wall | JVM | CPU-s | cores busy | modification analyzer | GC |
|---|---|---|---|---|---|---|
| timefold | 1158 s | 1125 s | 5553 | 4.9 | 91.9% | 7.2% |
| guava | 746 s | 716 s | 3699 | 5.2 | 91.0% | 7.9% |
| jenkins | 257 s | 227 s | 689 | 3.0 | 74.7% | 20.6% |
| langchain4j | 216 s | 186 s | 471 | 2.5 | 78.7% | 18.2% |

Parse (`inspection/openjdk`), prep, the nullability pass and the printer (`cst/print`) stay under 2% of CPU on
every corpus. The time is the modification analysis.

Inclusive CPU share of the hot frames (a frame counts once per stack, so the rows overlap):

| frame | timefold | guava | jenkins | langchain4j |
|---|---|---|---|---|
| `SingleIterationAnalyzerImpl.processElement` | 89.3% | 88.4% | 70.9% | 77.6% |
| `LinkGraph.compute` | 56.2% | 35.3% | 31.1% | 58.8% |
| `IncrementalFixpointEngine.incrementalUpdate` | 52.9% | 33.4% | 27.2% | 40.5% |
| `HashMap.getNode` | 20.4% | 13.3% | 12.3% | 16.4% |
| `ObjectCreationVariableImpl.hashCode` | 17.4% | 6.0% | 0.9% | 3.5% |
| `ParameterizedTypeImpl.hashCode` | 15.7% | 6.6% | 1.8% | 3.9% |
| `TypeParameterImpl.hashCode` | 4.8% | 2.3% | 0.3% | 0.2% |
| `Either.hashCode` | 2.1% | 1.0% | 0.1% | 0.0% |

Allocation (sampled every 2 MB, timefold):
- The hash methods account for about 27%: `Objects.hash` allocates an argument array on every call.
- `Util.scopeVariables` 14.7%, `FieldReferenceImpl.computeFqn` 6.4%, `Fact.<init>` 6.5%.
- On jenkins, `FieldReferenceImpl.computeFqn` is the second site, at 9.8%.

## 3. Hash caching

`ObjectCreationVariableImpl`, `ParameterizedTypeImpl`, `TypeParameterImpl` and `Either` are immutable. Each recomputed
its hash on every call: `ParameterizedTypeImpl` recursing through its type arguments with `Objects.hash`, and
`ObjectCreationVariableImpl` adding its `fqn` string on top. The change caches each hash. It keeps the same values, so
hash-ordered iteration, and with it the analyzer's determinism, is unchanged.

Results on guava: §4.

## 4. After hash caching (guava)

maddi `18ed7a80a`, maddi-mod `3158698c`. Same machine and options as §2.

| guava | before | after | change |
|---|---|---|---|
| wall | 746 s | 730 s | −2.1% |
| JVM | 716 s | 697 s | −2.7% |
| CPU-s | 3699 | 3517 | −4.9% |
| allocation samples (2 MB each) | 1,800,181 | 1,573,316 | −12.6% |
| `ObjectCreationVariableImpl.hashCode` | 6.0% | 0.2% | |
| `ParameterizedTypeImpl.hashCode` | 6.6% | 0.1% | |
| `TypeParameterImpl.hashCode` | 2.3% | 0.0% | |
| `HashMap.getNode` | 13.3% | 13.2% | |
| `LinkGraph.compute` | 35.3% | 33.1% | |
| J2K: compiling files / type errors | 603 / 12 | 603 / 12 | unchanged |

What it means:
- The hash methods are gone from the profile, but they were worth less than their rows add up to. The shares are
  inclusive and overlap: `ObjectCreationVariableImpl.hashCode` contains `ParameterizedTypeImpl.hashCode`. The
  `HashMap` lookups and the `equals` calls around them remain.
- The run is parallel (5 cores busy), so the CPU saved shows only in part on the wall clock.
- Noise: the two baseline runs differed by 5 s (751 s, 746 s). One run each side, so the wall figure is indicative.
- Timefold, where the hashes were 16–17% each, should gain more. It was not measured here.

Next levers, in order of share:
- The link graph computation and its incremental fixpoint engine: 31–33% inclusive on guava, 53–56% on timefold.
  This is algorithmic work.
- `HashMap` traffic in that engine: `getNode` 13%.
- GC: 8% on guava, about 20% on jenkins and langchain4j. Driven by `Util.scopeVariables` and `FieldReferenceImpl`
  construction (`computeFqn`).
