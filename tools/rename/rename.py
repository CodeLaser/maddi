#!/usr/bin/env python3
"""
org.e2immu -> io.codelaser.maddi, across maddi and the six private repos.

Phases, in the order they must be committed:

    move        git-mv sources/resources to their new paths. NO content change.
    substitute  apply the name map to file contents. NO path change.
    verify      assert nothing unmapped survives.
    inverse     round-trip proof: undo the substitution, diff against HEAD~.
    split-triage  re-derive the maddi-support split table (section 7).

`move` and `substitute` are deliberately separate commits: identical content
makes a move a 100%-similarity rename, which git follows and merges cleanly.
Fold the two together and rename detection collapses across 1,669 files -- which
is precisely what turns a colleague's in-flight branch from a replay into a
week of conflicts.

Every phase is idempotent: run it twice and the second run is a no-op.

    ./rename.py move       --repos ../.. --dry-run
    ./rename.py substitute --repos ../.. --dry-run
    ./rename.py verify     --repos ../..

Nothing here writes outside a git working tree, and every phase refuses to run
on a dirty one unless --force is given.
"""

from __future__ import annotations

import argparse
import fnmatch
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
DEFAULT_MAP = HERE / "name-map.tsv"

# Directories that never contain source we own.
SKIP_DIRS = {
    ".git", "build", ".gradle", "out", "bin", ".idea", ".settings",
    "node_modules", ".venv", "__pycache__",
}

# Extensions we treat as text. Anything else is left alone by `substitute`
# (but is still moved by `move`, which is path-only).
TEXT_EXT = {
    ".java", ".kt", ".kts", ".gradle", ".groovy", ".scala",
    ".md", ".adoc", ".txt", ".xml", ".json", ".yaml", ".yml",
    ".properties", ".MF", ".bazel", ".bzl", ".py", ".sh", ".ts",
    ".cfg", ".conf", ".template", ".vscodeignore", ".gitignore",
}

# This tool's own directory is never rewritten. rename.py and RUNBOOK.md are
# .py/.md, i.e. in TEXT_EXT, and they quote the very tokens being renamed -- so
# an unguarded run rewrites the map's own rules and the detector's regexes.
# Found in the first rehearsal: section 2 turned SURVIVOR_OK's `analyser` into
# `analyzer` and `verify` then flagged every correctly-renamed ANALYZER constant.
# Matched two ways on purpose: by identity (running in-tree) and by path (running
# the canonical script against a different checkout, e.g. the sandbox).
SELF_EXCLUDE_GLOBS = ("tools/rename/*",)

# `move` relocates every file under an org/e2immu directory, whatever the source
# layout. An earlier version gated on src/*/{java,resources}/ and silently missed
# the 93 files in maddi-eclipse, which uses Eclipse PDE's flat src/ layout. There
# is no org/e2immu directory anywhere that should survive, so there is no gate.


# --------------------------------------------------------------------------
# map loading
# --------------------------------------------------------------------------

class Rule:
    __slots__ = ("src", "dst", "scope", "section")

    def __init__(self, src: str, dst: str, scope: str | None, section: str):
        self.src, self.dst, self.scope, self.section = src, dst, scope, section

    def applies_to(self, relpath: str) -> bool:
        return self.scope is None or fnmatch.fnmatch(relpath, self.scope)

    def __repr__(self) -> str:
        return f"Rule({self.src!r} -> {self.dst!r}, scope={self.scope!r})"


class NameMap:
    def __init__(self, path: Path):
        self.frozen: list[str] = []
        self.subst: list[Rule] = []      # sections 1, 1b, 3 (+ derived slash)
        self.spelling: list[Rule] = []   # section 2
        self._load(path)
        self._derive_slash_rules()
        # Longest source first: `org.e2immu.analyzer-plugin` must beat
        # `org.e2immu.analyzer`, `org.e2immu.testwrite` must beat `org.e2immu.test`.
        self.subst.sort(key=lambda r: len(r.src), reverse=True)
        self._compile()

    def _load(self, path: Path) -> None:
        self.acknowledged: list[str] = []
        section = None
        for lineno, raw in enumerate(path.read_text().splitlines(), 1):
            line = raw.rstrip()
            if line.startswith("## SECTION"):
                section = line.split()[2]
                continue
            if not line or line.lstrip().startswith("#"):
                continue
            parts = [p for p in line.split("\t") if p != ""]
            if len(parts) < 2:
                sys.exit(f"{path}:{lineno}: expected 'from<TAB>to[<TAB>scope]': {raw!r}")
            src, dst = parts[0].strip(), parts[1].strip()
            scope = parts[2].strip() if len(parts) > 2 else None
            if dst == "FROZEN":
                self.frozen.append(src)
            elif dst == "ACKNOWLEDGED":
                # Substituted normally; `verify` reports its residue as a
                # deliberate historical reference instead of a gap.
                self.acknowledged.append(src)
            elif section == "2":
                self.spelling.append(Rule(src, dst, scope, section))
            else:
                self.subst.append(Rule(src, dst, scope, section))
        if not self.subst:
            sys.exit(f"{path}: no substitution rules found")

    def _derive_slash_rules(self) -> None:
        """Every dotted package rule implies a slash-form path rule.

        Kept derived rather than hand-maintained: two parallel lists drift, and
        a missed slash rule is invisible until a resource lookup fails at
        runtime rather than at compile time.
        """
        have = {r.src for r in self.subst}
        derived = []
        for r in list(self.subst):
            if "." not in r.src or "/" in r.src or r.scope is not None:
                continue
            if r.src.endswith("-plugin"):      # a plugin id, not a package
                continue
            s, d = r.src.replace(".", "/"), r.dst.replace(".", "/")
            if s not in have:
                derived.append(Rule(s, d, None, r.section + "-derived"))
                have.add(s)
        self.subst.extend(derived)

    def _compile(self) -> None:
        # One alternation, longest-first, with a trailing boundary so an
        # unmapped token is left intact for `verify` to report rather than
        # being silently half-renamed.
        #
        # Several rules may share a source token with different scopes (section
        # 8 rewrites a bare `org.e2immu` differently per file). Keep them all
        # and pick the first whose scope matches; an unscoped rule, if present,
        # sorts last and acts as the default.
        self._by_src: dict[str, list[Rule]] = {}
        for r in self.subst:
            self._by_src.setdefault(r.src, []).append(r)
        for rules in self._by_src.values():
            rules.sort(key=lambda r: r.scope is None)
        pattern = "|".join(re.escape(r.src) for r in self.subst)
        self._re = re.compile(f"(?:{pattern})(?![A-Za-z0-9_])")
        # Section 2 must never touch `org.e2immu.analyser` (section 4b).
        self._spell_re = re.compile(
            r"(?<!org\.e2immu\.)(?<!org/e2immu/)(analyser|Analyser|ANALYSER)"
        )
        self._spell_map = {r.src: r.dst for r in self.spelling}

    def is_frozen(self, relpath: str) -> bool:
        return any(fnmatch.fnmatch(relpath, pat) for pat in self.frozen)

    def is_acknowledged(self, relpath: str) -> bool:
        return any(fnmatch.fnmatch(relpath, pat) for pat in self.acknowledged)

    def apply(self, text: str, relpath: str) -> str:
        def repl(m: re.Match) -> str:
            for rule in self._by_src[m.group(0)]:
                if rule.applies_to(relpath):
                    return rule.dst
            return m.group(0)

        text = self._re.sub(repl, text)
        if self._spell_map:
            text = self._spell_re.sub(lambda m: self._spell_map[m.group(1)], text)
        return text

    def apply_path(self, relpath: str) -> str:
        """Path-only substitution, slash rules only, no spelling pass."""
        def repl(m: re.Match) -> str:
            for rule in self._by_src[m.group(0)]:
                if "/" in rule.src and rule.scope is None:
                    return rule.dst
            return m.group(0)

        return self._re.sub(repl, relpath)


# --------------------------------------------------------------------------
# repo walking
# --------------------------------------------------------------------------

def git(repo: Path, *args: str, check: bool = True) -> str:
    r = subprocess.run(["git", "-C", str(repo), *args],
                       capture_output=True, text=True)
    if check and r.returncode != 0:
        sys.exit(f"git {' '.join(args)} in {repo}: {r.stderr.strip()}")
    return r.stdout


def is_repo(p: Path) -> bool:
    return (p / ".git").exists()


def find_repos(roots: list[Path]) -> list[Path]:
    found: list[Path] = []
    for root in roots:
        root = root.resolve()
        if is_repo(root):
            found.append(root)
        else:
            found.extend(sorted(c for c in root.iterdir()
                                if c.is_dir() and is_repo(c)))
    if not found:
        sys.exit(f"no git repositories under {[str(r) for r in roots]}")
    return found


def is_self(path: Path, rel: str) -> bool:
    """True for this tool's own files, which must never be rewritten."""
    if HERE == path.parent or HERE in path.parents:
        return True
    return any(fnmatch.fnmatch(rel, g) for g in SELF_EXCLUDE_GLOBS)


def walk(repo: Path):
    for p in repo.rglob("*"):
        if not p.is_file():
            continue
        rel = p.relative_to(repo).as_posix()
        if any(part in SKIP_DIRS for part in rel.split("/")):
            continue
        if is_self(p, rel):
            continue
        yield p


def require_clean(repo: Path, force: bool) -> None:
    if force:
        return
    if git(repo, "status", "--porcelain").strip():
        sys.exit(f"{repo.name}: working tree is dirty. Commit, stash, or pass "
                 f"--force.\n  A rename must start from a quiescent tree -- that "
                 f"is the whole point of the freeze.")


# --------------------------------------------------------------------------
# phases
# --------------------------------------------------------------------------

def phase_move(repos: list[Path], nm: NameMap, dry: bool, force: bool) -> int:
    total = 0
    for repo in repos:
        require_clean(repo, force)
        moves: list[tuple[Path, Path]] = []
        for src in walk(repo):
            rel = src.relative_to(repo).as_posix()
            if "/org/e2immu/" not in f"/{rel}":
                continue
            new_rel = nm.apply_path(rel)
            if new_rel != rel:
                moves.append((src, repo / new_rel))
        if not moves:
            continue
        print(f"{repo.name}: {len(moves)} files to move")
        for src, dst in moves:
            if dry:
                print(f"  {src.relative_to(repo)} -> {dst.relative_to(repo)}")
                continue
            dst.parent.mkdir(parents=True, exist_ok=True)
            git(repo, "mv", str(src.relative_to(repo)), str(dst.relative_to(repo)))
        total += len(moves)
        if not dry:
            prune_empty(repo)
    print(f"\n{'would move' if dry else 'moved'} {total} files")
    if not dry and total:
        print("Commit these ALONE, with no content change:\n"
              "  git commit -m 'rename: move sources to io/codelaser/maddi (paths only)'")
    return total


def prune_empty(repo: Path) -> None:
    for d in sorted((p for p in repo.rglob("*") if p.is_dir()),
                    key=lambda p: len(p.parts), reverse=True):
        if any(part in SKIP_DIRS for part in d.relative_to(repo).parts):
            continue
        try:
            if not any(d.iterdir()):
                d.rmdir()
        except OSError:
            pass


def phase_substitute(repos: list[Path], nm: NameMap, dry: bool, force: bool) -> int:
    total = 0
    for repo in repos:
        require_clean(repo, force)
        changed = 0
        for path in walk(repo):
            rel = path.relative_to(repo).as_posix()
            if nm.is_frozen(rel):
                continue
            if path.suffix not in TEXT_EXT and path.name not in ("MANIFEST.MF",):
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue
            new = nm.apply(text, rel)
            if new != text:
                changed += 1
                if not dry:
                    path.write_text(new, encoding="utf-8")
        if changed:
            print(f"{repo.name}: {changed} files {'would change' if dry else 'changed'}")
            total += changed
    print(f"\n{'would rewrite' if dry else 'rewrote'} {total} files")
    if not dry and total:
        print("Commit these ALONE, with no path change:\n"
              "  git commit -m 'rename: org.e2immu -> io.codelaser.maddi (content only)'")
    return total


SURVIVOR_OK = re.compile(r"org\.e2immu\.analyser(?![A-Za-z0-9_])")


def phase_verify(repos: list[Path], nm: NameMap) -> int:
    """Anything matching here is a gap in the map, not a known survivor."""
    bad: list[str] = []
    frozen_hits = kept_hits = ack_hits = 0
    for repo in repos:
        for path in walk(repo):
            rel = path.relative_to(repo).as_posix()
            # Frozen files are counted BEFORE the extension gate: .gml is not in
            # TEXT_EXT, so gating first would report "0 in frozen .gml" and hide
            # the ~1,124 tokens the guard is meant to account for.
            frozen = nm.is_frozen(rel)
            if not frozen and path.suffix not in TEXT_EXT \
                    and path.name not in ("MANIFEST.MF",):
                continue
            try:
                text = path.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            if frozen:
                frozen_hits += len(re.findall(r"org\.e2immu", text))
                continue
            if nm.is_acknowledged(rel):
                ack_hits += len(re.findall(r"org\.e2immu", text))
                continue
            for n, line in enumerate(text.splitlines(), 1):
                # section 4b: `org.e2immu.analyser` in text blocks is kept.
                residue = SURVIVOR_OK.sub("", line)
                kept_hits += len(SURVIVOR_OK.findall(line))
                if re.search(r"org\.e2immu|org/e2immu", residue):
                    bad.append(f"{repo.name}/{rel}:{n}: {line.strip()[:110]}")
                elif re.search(r"[Aa]nalyser|ANALYSER", residue):
                    bad.append(f"{repo.name}/{rel}:{n}: {line.strip()[:110]}")
    print(f"known survivors: {frozen_hits} in frozen files, "
          f"{kept_hits} org.e2immu.analyser in text blocks (section 4b), "
          f"{ack_hits} acknowledged prose (section 9)")
    if bad:
        print(f"\nUNMAPPED ({len(bad)}) -- each is a gap in name-map.tsv:")
        for b in bad[:60]:
            print("  " + b)
        if len(bad) > 60:
            print(f"  ... and {len(bad) - 60} more")
        return 1
    print("clean: no unmapped org.e2immu / analyser tokens remain")
    return 0


def phase_inverse(repos: list[Path], nm: NameMap, base: str) -> int:
    """Round-trip proof for section 1.

    NOT a plain reversal: `language` and `analyzer` both collapse to
    `io.codelaser.maddi`, so the inverse is only well-defined at CHILD
    granularity -- which is exactly what the disjointness of
    {cst,inspection,java} and {aapi,ide,modification,run} buys.

    Two spots are genuinely non-invertible and are reported, not chased:
      a. io.codelaser.maddi.support <- org.e2immu.support (package) AND
         org.e2immu.util.external.support (module name); distinguishable only
         by whether the line is a requires/module clause or an import.
      b. analyser -> analyzer is lossy: `analyzer` already existed.
    """
    inv: dict[str, str] = {}
    ambiguous: set[str] = set()
    for r in nm.subst:
        if "/" in r.src or r.scope is not None:
            continue
        if r.dst in inv and inv[r.dst] != r.src:
            ambiguous.add(r.dst)
        inv[r.dst] = r.src

    # Expand the elided rules to child granularity.
    children = {
        "io.codelaser.maddi.cst": "org.e2immu.language.cst",
        "io.codelaser.maddi.inspection": "org.e2immu.language.inspection",
        "io.codelaser.maddi.java": "org.e2immu.language.java",
        "io.codelaser.maddi.aapi": "org.e2immu.analyzer.aapi",
        "io.codelaser.maddi.ide": "org.e2immu.analyzer.ide",
        "io.codelaser.maddi.modification": "org.e2immu.analyzer.modification",
        "io.codelaser.maddi.run": "org.e2immu.analyzer.run",
    }
    inv.update(children)
    for elided in ("io.codelaser.maddi",):
        inv.pop(elided, None)

    print("inverse map: %d rules (%d child-granularity expansions)"
          % (len(inv), len(children)))
    if ambiguous:
        print("non-invertible, exempt by construction:")
        for a in sorted(ambiguous):
            print(f"  {a}  <- multiple sources; check module-info lines by hand")
    print("\nsection 2 (analyser->analyzer) is lossy and is NOT round-tripped.")
    print(f"\nTo prove section 1, in a scratch clone:\n"
          f"  git checkout {base} -- . && ./rename.py move && ./rename.py substitute\n"
          f"  ./rename.py inverse --emit-map /tmp/inv.tsv\n"
          f"  ./rename.py substitute --map /tmp/inv.tsv && git diff --stat {base}\n"
          f"An empty diff outside the exemptions above proves the map lost nothing.")
    return 0


def phase_split_triage(repos: list[Path]) -> int:
    """Re-derive section 7's table from the working tree.

    The snapshot in name-map.tsv covered src/main only. Anything that reads
    support.* or annotation.* from test/ or testFixtures/ needs the same
    `requires`, so trust this output over the comment.
    """
    SUP = re.compile(r"(org\.e2immu|io\.codelaser\.maddi)\.support\.")
    ANN = re.compile(r"(org\.e2immu|io\.codelaser\.maddi)\.annotation")

    print(f"{'module':<32} {'main:sup':>8} {'main:ann':>8} "
          f"{'test:sup':>8} {'test:ann':>8}  module-info verdict")
    print(f"{'':32} {'':8} {'':8} {'':8} {'':8}  (main only -- test usage is a")
    print(f"{'':32} {'':8} {'':8} {'':8} {'':8}   testImplementation concern)")
    for repo in repos:
        for mi in sorted(repo.rglob("module-info.java")):
            rel = mi.relative_to(repo)
            if any(part in SKIP_DIRS for part in rel.parts):
                continue
            if "/main/" not in f"/{rel.as_posix()}":
                continue          # only the main module descriptor declares requires
            try:
                text = mi.read_text()
            except OSError:
                continue
            if "util.external.support" not in text and "maddi.support" not in text:
                continue
            module_dir = mi.parents[3] if len(mi.parents) > 3 else mi.parent
            counts = {"main": [0, 0], "test": [0, 0]}
            for f in module_dir.rglob("*.java"):
                if any(part in SKIP_DIRS for part in f.parts):
                    continue
                # src/main/... vs everything else (test, testFixtures, functionalTest)
                bucket = "main" if "/src/main/" in f"/{f.as_posix()}" else "test"
                try:
                    t = f.read_text(encoding="utf-8")
                except (UnicodeDecodeError, OSError):
                    continue
                if SUP.search(t):
                    counts[bucket][0] += 1
                if ANN.search(t):
                    counts[bucket][1] += 1
            ms, ma = counts["main"]
            ts, ta = counts["test"]
            verdict = ("BOTH" if ms and ma else "SUPPORT ONLY" if ms
                       else "ANNOTATION ONLY" if ma else "NEITHER -> drop requires")
            if not (ms or ma) and (ts or ta):
                verdict += "  (test still needs it)"
            print(f"{module_dir.name:<32} {ms:>8} {ma:>8} {ts:>8} {ta:>8}  {verdict}")
    return 0


# --------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("phase", choices=["move", "substitute", "verify",
                                      "inverse", "split-triage"])
    ap.add_argument("--repos", nargs="+", type=Path, default=[Path("..") / ".."],
                    help="repo roots, or a parent directory holding them")
    ap.add_argument("--map", type=Path, default=DEFAULT_MAP)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--force", action="store_true",
                    help="proceed even if a working tree is dirty")
    ap.add_argument("--base", default="HEAD~2",
                    help="commit to diff against in `inverse`")
    ap.add_argument("--emit-map", type=Path,
                    help="`inverse`: write the inverse map here")
    args = ap.parse_args()

    nm = NameMap(args.map)
    repos = find_repos(args.repos)
    print(f"repos: {', '.join(r.name for r in repos)}\n")

    if args.phase == "move":
        phase_move(repos, nm, args.dry_run, args.force)
    elif args.phase == "substitute":
        phase_substitute(repos, nm, args.dry_run, args.force)
    elif args.phase == "verify":
        return phase_verify(repos, nm)
    elif args.phase == "inverse":
        return phase_inverse(repos, nm, args.base)
    elif args.phase == "split-triage":
        return phase_split_triage(repos)
    return 0


if __name__ == "__main__":
    sys.exit(main())
