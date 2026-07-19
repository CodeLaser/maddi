# Partial re-parse / re-wire — emitting `.class` from the openjdk inspection pass

**Status:** investigation / spike notes. Not implemented. Target: a separate feature in the coming weeks.
**Date:** 2026-06.
**Scope:** `maddi-java-openjdk` (`ScanCompilationUnits`) and `maddi-inspection-openjdk` (`JavaInspectorImpl`).

> Note on location: this file lives in the `jfocus-refactor-service` repo root because that was the active
> working context, but the code it describes is entirely in **maddi**. Move it under maddi if that's a better home.

---

## 1. Motivation

We use the openjdk parser to analyze code, then make changes (refactorings / method splits). After an edit we
must **re-parse** to get a fresh, correct CST. But re-parsing a *dependent* source set requires the *depended-on*
source set to be available as **`.class` files** on the classpath — so today the loop is:

```
edit  →  recompile (javac)  →  re-parse (maddi)  →  edit  →  ...
```

On the real target codebase:

- **~3.5M lines**, **2 source sets** (a monolith) with heavy interdependency ("spaghetti", hence a lot of
  preloading of referenced classes).
- A full `javac` recompile takes **~2 minutes**.

Goal: **minimize the downtime between two editing sessions**. The standalone 2-minute recompile is the obvious
thing to attack.

---

## 2. Why `.class` files are needed at all

maddi parses each source set in its **own `JavacTask`** (`JavaInspectorImpl.createTask`). When it parses source
set *B* that depends on *A*, javac resolves references into *A* by reading **`A`'s `.class` files** from the
classpath via `ClassSymbolScanner` (the "preloading"), **not** from maddi's already-built CST of *A*. So:

> After editing *A*, *A* must be (re)compiled to `.class` before *B* can be (re)parsed.

That recompile is the cost we want to remove or shrink.

---

## 3. Current parse pipeline (relevant parts)

- `JavaInspectorImpl.singleSourceSet(...)` (≈ lines 266–308):
  1. `createTask(...)` builds a `JavacTask` (≈ lines 310–391).
     - **The `StandardJavaFileManager` is opened in a try-with-resources and closed when `createTask` returns**,
       i.e. *before* `scan()` runs.
     - Compiler options (line ~387): `-proc:none --enable-preview --release=26 -parameters`.
     - **No `-d` / `CLASS_OUTPUT` is set.**
  2. `ScanCompilationUnits.scan()` runs `task.parse()` then `task.analyze()` (≈ lines 89–91), checks diagnostics,
     does preloading + the CST scan + javadoc, returns the `Result`.
  3. Back in `singleSourceSet`, a **commit loop** (≈ lines 300–307) calls
     `classSymbolScanner().commitType(typeInfo)` — which **still uses the task** (`elements.getTypeElement(...)`).
- Source sets are processed in **dependency order** already (`Linearize` in `JavaInspectorImpl`).
- `ParseOptions` (record in `JavaInspector`): `failFast, detailedSources, invalidated, parallel, lombok,
  ignoreModule, parameterNames`. Note the existing **`invalidated` / `InvalidationState`** hook.

The natural place to obtain `.class` files is javac's third phase: **`JavacTask.generate()`**
(`Iterable<? extends JavaFileObject> generate()`), which runs after `analyze()`.

---

## 4. The spike

**Test used:** `maddi-inspection-openjdk` → `TestJavaInspector4RealClasspath` — parses
`../maddi-cst-api/src/main/java` from a **real source directory** (not the in-memory test protocol), with
`failFast(true)` and the real dependencies (javaBase, annotations, maddi-support), so `analyze()` succeeds.

A temporary `task.generate()` call (wrapped in try/catch + logging) was inserted at three positions:

| Placement of `generate()`                                  | Result |
|------------------------------------------------------------|--------|
| Immediately after `task.analyze()` (inside `scan()`)        | ❌ `NullPointerException`: `BasicJavacTask.getContext()` returns **null**, breaking the very next step, `indexJavaLangForJavaDocParsing` (preloading). |
| End of `scan()` (after preload + CST scan + javadoc)        | ❌ `PropagatedException`/`IllegalStateException` from `JavacElements.ensureEntered` → `prepareCompiler`, hit by the **post-scan `commitType` loop** in `singleSourceSet` (`elements.getTypeElement`). `generate()` itself reported **OK: 278 class files in ~162 ms.** |
| **End of `singleSourceSet`, after the commit loop**         | ✅ Test **green**. `generate()` OK: **278 class files in ~140 ms**, written **next to the sources** (e.g. `maddi-cst-api/src/main/java/.../Value$SetOfTypeInfo.class`). |

(The 278 generated `.class` files were deleted and all spike edits reverted afterwards.)

---

## 5. Key findings

1. **`generate()` works with the existing setup — no file-manager rework needed.** Despite `createTask` closing
   the `StandardJavaFileManager`, codegen still wrote bytecode fine. The earlier worry about a closed `fm` was a
   non-issue.

2. **Codegen is cheap.** ~140 ms for 278 files. It is the small tail phase on top of `parse()`+`analyze()`, which
   we already pay for. Folding it into the existing parse is far cheaper than a separate full `javac`.

3. **Output defaults to next-to-source.** With no `-d`/`CLASS_OUTPUT`, javac uses the **sibling** of each source
   file. That's the desired behavior for keeping a source tree's `.class` in sync (and explains stray `.class`
   files appearing next to `.java`). Set `StandardLocation.CLASS_OUTPUT` if a separate output dir is wanted.

4. **`generate()` is terminal — it tears down the `JavacTask`.** Afterwards `getContext()` is null and any
   re-entry (`getElements().getTypeElement(...)`) throws. Therefore it must be the **very last operation that
   touches the task** — later than `scan()`, after `singleSourceSet`'s commit loop.

5. **A task is single-use.** You cannot keep one `JavacTask` warm across edit sessions and re-emit. Each parse
   round needs a fresh task. (Keep the *CST* warm, not the task.)

---

## 6. Design options

### A. Piggyback `generate()` on the parse (cheap win)
Emit `.class` as a byproduct of the parse we already run, eliminating the standalone 2-minute compile. Because
source sets are linearized, parsing the leaf set first emits its `.class`, which the dependent set's parse then
reads. Removes the *separate* compile but still re-parses everything.

### B. Incremental re-parse via `ParseOptions.invalidated` (the bigger lever)
Keep the inspector **warm in-process** across sessions (CST + `compiledTypesManager` in memory). On an edit:
- re-parse **only the changed files plus their invalidation closure**, feeding the rest as existing `.class` on
  the classpath;
- emit `.class` only for the recompiled units (a task containing only the changed files generates only those).

Per-edit downtime becomes proportional to the change set, not 3.5M lines. Unchanged `.class` simply persist on
disk between sessions — exactly what the next parse's classpath wants.

### C. Separate incremental `javac` pass (most robust)
Run codegen as its own task (`-d <outdir>`, only changed files, rest on classpath), fully decoupled from the
inspection task. No dead-task hazard, no ordering coupling. Costs a second parse+analyze of the changed files,
which is cheap when the delta is small. Prefer this if option A's lazy-load risk (below) materializes.

**Recommended:** combine **B** (incremental) with either **A** (free `.class` during the incremental parse) or
**C** (decoupled codegen) depending on how the lazy-load risk shakes out.

---

## 7. Open questions / risks to validate

1. **Multi-source-set lazy loading (highest priority).** `ClassSymbolScanner` lazily loads referenced types
   *through the task* on demand. If parsing *B* pulls an *A* type through *A*'s already-`generate()`d (dead) task,
   it will throw. The single-source-set spike can't show this. **Next step: run the same spike against
   `TestJavaInspector6MultiProject`** (7 dependent source sets). If green → end-of-`singleSourceSet` placement is
   viable. If it throws → defer all `generate()` calls to the very end of the *whole* parse, or use option C.
2. **Invalidation correctness.** If an edited file's **public API** changes, dependents must be re-parsed/re-emitted
   too. The invalidation closure must track API-level changes, not just the edited file.
3. **Preview flag.** `--enable-preview --release=26` ⇒ emitted classes carry the preview minor-version marker.
   Fine for maddi/javac re-reading; running on a stock JVM needs `--enable-preview`.
4. **Output location per source set.** Decide between next-to-source (default) and an explicit `CLASS_OUTPUT`
   directory per source set; the latter is cleaner for an incremental build cache.

---

## 8. Concrete implementation notes (when picking it up)

- Add a `generateClassFiles` flag to `JavaInspector.ParseOptions` (default **false**); thread it through
  `singleSourceSet`.
- Call `javacTask.generate()` at the **end of `singleSourceSet`**, after the commit loop — *not* inside `scan()`.
  (If the multi-set lazy-load test fails, move it to after the whole linearized parse instead.)
- **Skip generation for `TEST_PROTOCOL`** (in-memory sources have no sibling directory; codegen would fail or
  write to cwd).
- Optionally `fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(dir))` for an explicit output dir.
- Re-check diagnostics after `generate()` (it can emit its own).

---

## 9. One-line summary

`JavacTask.generate()` is a cheap, working way to get `.class` out of the parse we already run (~140 ms/278 files,
next-to-source) — **but it destroys the task**, so it must run as the last task operation (end of
`singleSourceSet`), behind a flag, skipping the in-memory test protocol. The real downtime win comes from pairing
it with **incremental re-parse** (`ParseOptions.invalidated`) so only the edited files are re-parsed and
re-emitted. The main thing left to verify is multi-source-set lazy loading against an already-generated task.
