#!/usr/bin/env python3
"""The tier check for the maddi split (docs/roadmap/split-maddi-into-three-repositories.md).

    check_tiers.py [--verbose] [ROOT ...]

ROOT defaults to this repository; pass the sibling checkouts (../maddi-mod, ../maddi-dist) once the split
has happened, so one run sees every module. Exit 0 when no edge breaks the rules in tiers.txt, 1 otherwise,
2 on a usage or configuration error.

An EDGE is read from three places, because each misses something the others see:
  * build.gradle.kts   `<configuration>(project(":m"))`, `<configuration>(testFixtures(project(":m")))` and,
                       after the split, `<configuration>("io.codelaser:m:…")` coordinates;
  * module-info.java   `requires [transitive|static] <jpms name>` (main scope);
  * source imports     `import [static] io.codelaser.maddi.…` in src/main, src/test and src/testFixtures,
                       mapped to the module whose src/main declares the package. This is the only source for
                       modules without a descriptor (the plugins, the Kotlin modules).

A configuration is COMPILE scope unless it is a runtime one: runtimeOnly, testRuntimeOnly, and any custom
configuration (shade, k2Runtime, …) that no `extendsFrom(...)` in the same file pulls into a compile one.
"""
import collections
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_ROOT = os.path.dirname(os.path.dirname(HERE))
RANK = {"base": 0, "mod": 1, "dist": 2}
COMPILE_CONFIGS = {"api", "implementation", "compileOnly", "compileOnlyApi", "annotationProcessor",
                   "testImplementation", "testCompileOnly", "testFixturesApi", "testFixturesImplementation",
                   "testFixturesCompileOnly", "testAnnotationProcessor"}
RUNTIME_CONFIGS = {"runtimeOnly", "testRuntimeOnly", "testFixturesRuntimeOnly"}
DEP_RE = re.compile(r'^\s*([A-Za-z]+)\(\s*(?:testFixtures\(\s*)?(?:project\(\s*":([A-Za-z0-9_-]+)"\s*\)'
                    r'|"io\.codelaser:([A-Za-z0-9_-]+):[^"]*")')
EXTENDS_RE = re.compile(r'configurations\.named\("([A-Za-z]+)"\)\s*\{\s*extendsFrom\(([A-Za-z]+)\)')
MODULE_RE = re.compile(r'^\s*(?:open\s+)?module\s+([\w.]+)', re.M)
REQUIRES_RE = re.compile(r'^\s*requires\s+(?:transitive\s+|static\s+)*([\w.]+)\s*;', re.M)
PACKAGE_RE = re.compile(r'^\s*package\s+([\w.]+)', re.M)
IMPORT_RE = re.compile(r'^\s*import\s+(?:static\s+)?(io\.codelaser\.maddi\.[\w.]+(?:\.\*)?)', re.M)


def read_tiers(path):
    tiers, required = {}, []
    for n, line in enumerate(open(path), 1):
        line = line.split("#", 1)[0].split()
        if not line:
            continue
        if line[0] == "require-runtime" and len(line) == 2:
            required.append(line[1])
        elif len(line) == 2 and line[1] in RANK:
            tiers[line[0]] = line[1]
        else:
            sys.exit(f"{path}:{n}: cannot read {' '.join(line)!r}")
    return tiers, required


def modules(roots):
    """{module name: directory} for every directory under a root that has a build.gradle.kts."""
    found = {}
    for root in roots:
        for name in sorted(os.listdir(root)):
            d = os.path.join(root, name)
            if os.path.isfile(os.path.join(d, "build.gradle.kts")) and name not in ("buildSrc", "build-logic"):
                if name in found:
                    sys.exit(f"module {name} is in two roots: {found[name]} and {d}")
                found[name] = d
    return found


def source_files(d, sets):
    for s in sets:
        base = os.path.join(d, "src", s)
        for dirpath, _, files in os.walk(base):
            for f in files:
                if f.endswith((".java", ".kt")):
                    yield s, os.path.join(dirpath, f)


def main(argv):
    verbose = "--verbose" in argv
    roots = [os.path.abspath(a) for a in argv[1:] if a != "--verbose"] or [DEFAULT_ROOT]
    tiers, required = read_tiers(os.path.join(DEFAULT_ROOT, "build-logic", "src", "main", "resources", "tiers.txt"))
    mods = modules(roots)

    problems = []
    untiered = sorted(m for m in mods if m not in tiers)
    for m in untiered:
        problems.append(f"UNTIERED   {m}: add it to tiers.txt")

    # package -> owning module, JPMS name -> module
    owner, jpms = {}, {}
    for m, d in mods.items():
        info = os.path.join(d, "src", "main", "java", "module-info.java")
        if os.path.isfile(info):
            mm = MODULE_RE.search(open(info).read())
            if mm:
                jpms[mm.group(1)] = m
        for _, f in source_files(d, ["main"]):
            if f.endswith("module-info.java"):
                continue
            pm = PACKAGE_RE.search(open(f, errors="replace").read())
            if pm:
                owner.setdefault(pm.group(1), set()).add(m)

    def owning_module(imported):
        name = imported[:-2] if imported.endswith(".*") else imported
        parts = name.split(".")
        for i in range(len(parts), 0, -1):
            hit = owner.get(".".join(parts[:i]))
            if hit:
                return hit
        return set()

    # edges: (from, to) -> list of (scope, kind, detail)
    edges = collections.defaultdict(list)
    runtime_edges = collections.defaultdict(set)
    for m, d in mods.items():
        text = open(os.path.join(d, "build.gradle.kts")).read()
        extended = {child for parent, child in EXTENDS_RE.findall(text) if parent in COMPILE_CONFIGS}
        for line in text.splitlines():
            if line.lstrip().startswith("//"):
                continue
            dm = DEP_RE.match(line)
            if not dm:
                continue
            conf, target = dm.group(1), dm.group(2) or dm.group(3)
            if target not in mods and target not in tiers:
                continue
            kind = "compile" if conf in COMPILE_CONFIGS or conf in extended else "runtime"
            scope = "test" if conf.startswith("test") else "main"
            edges[(m, target)].append((scope, kind, f"build: {conf}" + (" (extended by a compile configuration)" if conf in extended else "")))
            if kind == "runtime":
                runtime_edges[m].add(target)
        info = os.path.join(d, "src", "main", "java", "module-info.java")
        if os.path.isfile(info):
            for req in REQUIRES_RE.findall(open(info).read()):
                if req in jpms and jpms[req] != m:
                    edges[(m, jpms[req])].append(("main", "compile", f"requires {req}"))
        for s, f in source_files(d, ["main", "test", "testFixtures"]):
            for imp in IMPORT_RE.findall(open(f, errors="replace").read()):
                for t in owning_module(imp):
                    if t != m:
                        edges[(m, t)].append(("main" if s == "main" else "test", "compile",
                                              f"import in {os.path.relpath(f, d)}"))

    # rules
    for (a, b), uses in sorted(edges.items()):
        ta, tb = tiers.get(a), tiers.get(b)
        if ta is None or tb is None:
            continue
        bad = []
        for scope, kind, detail in uses:
            if RANK[tb] > RANK[ta]:
                bad.append((scope, kind, detail))
            elif ta == "dist" and tb == "mod" and kind == "compile":
                bad.append((scope, kind, detail))
        if bad:
            scopes = sorted({s for s, _, _ in bad})
            kinds = collections.Counter(d.split(" in ")[0].split(":")[0] for _, _, d in bad)
            summary = ", ".join(f"{k} x{n}" for k, n in sorted(kinds.items()))
            problems.append(f"VIOLATION  {a} ({ta}) -> {b} ({tb})  [{'/'.join(scopes)}]  {summary}")
            if verbose:
                for s, k, d in sorted(set(bad)):
                    problems.append(f"             {s:5} {k:8} {d}")

    for m in required:
        if m not in mods:
            continue
        if not any(tiers.get(t) == "mod" for t in runtime_edges[m]):
            problems.append(f"NO-RUNTIME {m} (dist) carries no mod module in a runtime-only configuration")

    for p in problems:
        print(p)
    n_viol = sum(p.startswith(("VIOLATION", "NO-RUNTIME", "UNTIERED")) for p in problems)
    print(f"{len(mods)} modules in {len(roots)} root(s); {len(edges)} module pairs with an edge; "
          f"{n_viol} problem(s)")
    return 1 if n_viol else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
