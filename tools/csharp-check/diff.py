#!/usr/bin/env python3
"""The difference between two runs of csharp-check: which files gained or lost which diagnostics.

    diff.py <before.out> <after.out> [E|S|N]

The arguments are two csharp-check.out files (build/j2cs/<corpus>/csharp-check.out of a ratchet run, or a copy
of one), the kind of line to compare: E errors (the default), S syntax errors, N nullable warnings. The output
has the totals, the per-code totals that moved, and one line per file and code that moved, the file relative to
its src/ directory, so that runs on different machines compare. A translation that got better and worse at once
shows both, which the ratchet's totals do not.
"""
import collections
import re
import sys


def load(path, kind):
    per_file = collections.Counter()
    per_code = collections.Counter()
    for line in open(path, encoding="utf-8"):
        f = line.rstrip("\n").split("\t")
        if len(f) < 6 or f[0] != kind:
            continue
        name = re.sub(r"^.*/src/", "", f[1])
        per_file[(name, f[4])] += 1
        per_code[f[4]] += 1
    return per_file, per_code


def main(argv):
    if len(argv) not in (3, 4):
        print(__doc__, file=sys.stderr)
        return 2
    kind = argv[3] if len(argv) == 4 else "E"
    before_files, before_codes = load(argv[1], kind)
    after_files, after_codes = load(argv[2], kind)
    print(f"{kind}: {sum(before_files.values())} -> {sum(after_files.values())}")
    moved = [(c, before_codes[c], after_codes[c]) for c in sorted(set(before_codes) | set(after_codes))
             if before_codes[c] != after_codes[c]]
    if moved:
        print("by code:")
        for code, was, now in sorted(moved, key=lambda m: m[2] - m[1]):
            print(f"  {now - was:+d}\t{was} -> {now}\t{code}")
    rows = [(k, before_files[k], after_files[k]) for k in sorted(set(before_files) | set(after_files))
            if before_files[k] != after_files[k]]
    if rows:
        print("by file:")
        for (name, code), was, now in sorted(rows, key=lambda r: (r[2] - r[1], r[0])):
            print(f"  {now - was:+d}\t{was} -> {now}\t{code}\t{name}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
