# Kotlin stdlib / library extension calls — open design question

Status: **blocked, needs a decision.** Parked 2026-07-01. Not yet implemented; a clean attempt was
made and reverted (see "What was tried").

## The gap

Idiomatic Kotlin leans on stdlib extension functions: `list.map { }`, `.filter { }`, `.first()`,
`.count()`, `.isNotEmpty()`, `s.trim()`, etc. These are **top-level extension functions in the Kotlin
stdlib**, compiled into per-package JVM *facade* classes (`kotlin.collections.CollectionsKt`,
`MapsKt`, `kotlin.text.StringsKt`, …). A call `recv.ext(args)` compiles to a **static** call
`Facade.ext(recv, args)` (the receiver becomes argument 0).

Today such calls fall to a `k2-unresolved-call:<name>` placeholder. The front-end already resolves
**source-defined** extensions (via the synthesized `<File>Kt` facade — see `extensionCall` /
`extensionFacade` in `KotlinBodyConverter`), but not **library** ones.

## Why it's blocked

The extension symbol is easy to detect: `KaNamedFunctionSymbol` with a non-null `receiverParameter`,
`psi == null` (compiled), and a `callableId` such as `kotlin/collections/count`. The problem is
finding the **JVM facade class** to route the static call to:

1. `callableId` gives only the **package** (`kotlin.collections`), and a package maps to *several*
   facades (`CollectionsKt`, `MapsKt`, `SetsKt`, `ArraysKt`, …). Which one holds `count` is not
   derivable from the callable.
2. `findClass(ClassId.topLevel(FqName("kotlin.collections.CollectionsKt")))` returns **null** — for
   every facade candidate. Stdlib facades are JVM-synthetic `@JvmMultifileClass` classes, **not
   declared `KaClassSymbol`s**, so K2's symbol API can't load them.
3. The real facade name lives in FIR-internal `containerSource`
   (`JvmPackagePartSource.facadeClassName`), which is **not exposed** in the public `KaSession` API.

## The fork

- **Option A — synthesize the facade from the symbol.** We have the extension's params (receiver +
  value params) and return type, so we can build a synthetic facade `TypeInfo` + static method and
  route the call. *Pro:* calls resolve now. *Con:* we can't obtain the real facade FQN, so either we
  **collide** with the AAPI's `CollectionsKt` on the shared `infoByFqn` registry (cross-language
  inconsistency — the same risk we avoided for mapped types), or we use a synthetic umbrella FQN and
  the **AAPI's modification/immutability annotations don't apply** (the analyzer sees the call
  structurally but its behaviour is unknown/conservative).
- **Option B — FIR-internal facade resolution.** Reach into
  `(symbol as KaFirNamedFunctionSymbol).firSymbol.fir.containerSource` for the real facade class name,
  then load its bytecode directly (not via K2 symbols). *Pro:* correct, AAPI-aligned. *Con:* fragile
  internal API; a separate bytecode-loading path.

## The open question (for the maddi maintainer)

This turns on the **AAPI** (annotated API), which the front-end author knows and the implementer does
not: **does maddi already provide annotated stdlib facades (`CollectionsKt`, `StringsKt`, …), and
under what FQNs?**

- If yes, the Kotlin front-end should resolve library extension calls to **those** shared TypeInfos
  (so the modification/immutability annotations apply) — which likely needs Option B's real facade
  name, and a hand-off to the AAPI loader rather than synthesis.
- If no, Option A with a synthetic umbrella facade resolves the *structure* of the call, accepting
  conservative (un-annotated) behaviour — but beware registry collisions if the AAPI later defines the
  real facade under the same FQN.

Tentative recommendation: **do not ship Option A blind.** A registry collision on a shared type is
exactly the cross-language inconsistency we've been careful about. Decide the AAPI story first.

## What was tried (and reverted)

- Detected the library extension (`psi == null`, `receiverParameter != null`, `callableId`).
- Added `KotlinTypeMapper.loadTypeByJvmFqn(jvmFqn)` (findClass + loadLibraryType) and a
  `libraryExtensionCall` that tried a hardcoded per-package list of facade candidates.
- **Result:** `findClass` returned null for all facade candidates (point 2 above); the whole attempt
  was reverted. The two shakeout bug-fixes made alongside it (non-Int constants; `map[k] = v` → `put`)
  are unrelated and were kept (commit `49065a01`).

## Related

- `KotlinBodyConverter`: `convertCall`, `extensionCall`, `extensionFacade`, `resolveCallee`.
- `KotlinTypeMapper`: `loadLibraryType`, mapped-type loading via `findClass`.
- Memory: `kotlin-front-end`, `detailedsources-keying`.
