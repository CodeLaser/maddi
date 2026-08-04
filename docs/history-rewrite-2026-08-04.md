# History rewrite, 2026-08-04 — every commit SHA changed

**Status: note.** If you have a clone, a branch, or a written-down commit hash from before
2026-08-04, read this. Every commit in the repository was rewritten on that date and **no SHA from
before it is still valid**.

## What was done and why

The [customer-name scrub](opening-up-to-contributors.md#5-customer-name-scrub--done-2026-08-04)
removed the customer behind the private proving corpus from the working tree. That fixed the
present, not the past: the names were still in ~35 commits' file content and in 23 commit messages,
reachable to anyone who cloned the repository.

`git filter-repo` replaced them across all 2,187 commits and all 25 branches, blob contents and
commit messages alike. Done while the repository had 0 forks and 0 stars, which is the only moment
this is cheap.

Verified three ways, not one:

- the pickaxe (`git log --all -S<term>`) returns **0 commits** for every term;
- **all 13,512 blobs** in the object store were decompressed and scanned — **0 matching lines**;
- every commit message scanned — **0 matching lines**.

## What it cost, and what was done about it

**126 commit SHAs were cited across the documentation and code** — `sv-journal.md` alone cites 37,
`sv-engine-handoff.md` 28. Every one of them changed. All 177 citation sites were rewritten to the
new hashes in the same change that added this note, so the tree is self-consistent again.

For anything *outside* the repository — a note, a mail, an old branch — the maintainer holds the
full old→new map for all 2,188 commits. Ask.

**It is deliberately not committed here.** It is an index of pre-rewrite commit hashes, and a
forge can keep serving an unreferenced commit by its hash for a long time after a force-push;
publishing the list would be publishing a route back to the content this rewrite removed.

## If you have a clone or a worktree

The published branches were force-pushed. A normal `git pull` will try to merge two unrelated
histories — do not. Re-clone, or reset each branch:

```bash
git fetch --all
git checkout <branch> && git reset --hard origin/<branch>
```

Local branches that were never pushed have to be rebased onto the rewritten history by hand, or
recreated. The five local worktrees in use on 2026-08-04 were repointed at the time.

## Recovery

A complete bundle of the pre-rewrite history was taken first and verified
(`git bundle verify`), covering all refs. It lives outside the repository — ask the maintainer.
It is the only remaining copy of the old history, and it still contains the customer names, so it
must not be published.

## Do not do this twice

The rewrite is a one-off, justified by the repository being unforked and unstarred. The commit hook
in [`.githooks/`](../.githooks/) exists so that a second one is never needed: it refuses the names at
commit time, in file content and in the commit message. Enable it per clone and per worktree —

```bash
git config core.hooksPath .githooks
```

— and see [CONTRIBUTING.md §Names that must not appear](../CONTRIBUTING.md#names-that-must-not-appear).
