# Opening maddi up to contributors — analysis and plan

**Status: plan.** Written 2026-08-04, while maddi still has 0 stars, 0 forks, 0 issues and 0 pull
requests. That is the cheapest moment to make every decision below; each one gets more expensive
the day someone else shows up.

The question that started it: should the `.md` TODO files become GitHub issues, and should changes
go through pull requests? The rest of the CodeLaser workspaces (`jfocus-*`) are commercial and keep
working with `.md` files — nothing here applies to them.

---

## 1. Pull requests: yes, asymmetrically

**Contributors open PRs; the maintainer keeps pushing.** Routing your own `ws/<workspace>` → `devel`
merges through pull requests would be ceremony — self-review is not review, and it would slow the
loop that currently produces the work. Maintainer-pushes / contributor-PRs is the standard open
source shape. Revisit when a second person has commit rights.

**The blocker to clear first.** `main` is the default branch, the PR target in `build.yml`, and was
**12 commits behind `devel`** when this was written. An outsider forks `main`, branches from a stale
point, and the result has to be replanted onto `devel`. Decide one of:

- keep `main` as the contribution surface and merge `devel` → `main` on a regular cadence
  (**recommended** — `main` reading as "latest good" is what visitors expect); or
- make `devel` the default branch and the PR target.

---

## 2. Issues vs `.md`: split them, do not convert them

A wholesale conversion would destroy value. Three reasons, all measured on the tree:

1. **The documents are cited from production code.** 38 distinct `.md` filenames are referenced from
   `.java`/`.kt` sources — `rewiring.md` 27×, `analysis-rewiring.md` 23×, `eventual-info-hierarchy.md`
   21× — and `IsolationCore`, `FieldAnalyzerImpl` and `TypeEventualAnalyzerImpl` name their rationale
   documents directly. `doc-audit-2026-07-30.md` §2 already settled the principle: a comment pointing
   at a deleted file is worse than a closed document sitting in the tree. An issue number is worse
   still — a file travels with the checkout, is greppable and readable offline; an issue is none of
   those.
2. **They are not TODO lists.** They are long-form reasoning: measurements, design forks, and
   negative results recorded *so nobody re-runs them*. Closing an issue archives the reasoning out of
   the reading path.
3. **They are load-bearing for the AI workflow.** `CLAUDE.md` and `AGENTS.md` route assistants to
   these files by path.

And the issue-shaped material is far smaller than 89 files suggests: **only three files contain
checkbox TODOs at all** — 34 open boxes in `prep-analyzer hardening.md`, 19 in
`landing-surface-checklist.md`, 15 in `modification-link-analyzer hardening.md`. Everything else's
"openness" is prose inside a design document.

**The rule, then:** documents stay and remain the reasoning record; the discrete open items *inside*
them become issues. The checkbox is replaced by a link to the issue; the issue links back to the
document section for the why.

---

## 3. The plan, in four phases

### Phase 1 — make the repository safe to land on

- [x] PR target: **`main`**, kept current by merging `devel` → `main`. Both were pushed level
      on 2026-08-04, which removed the "12 behind" problem this phase existed to fix.
- [x] `.github/ISSUE_TEMPLATE/` — `analysis-verdict.yml` (the characteristic report for an
      analyzer: computed vs expected, with a distilled type), `bug.yml` (crashes and failures),
      and `config.yml` routing concept questions to Discussions. Both forms ask for version, JDK
      and front end; the bug form asks whether the JDK ships `jmods/`.
- [x] `.github/PULL_REQUEST_TEMPLATE.md` — the golden rule (byte-identical `FPDUMP` A/B for engine
      changes), the fast/slow split, the concept-changes-land-in-the-docs rule, and the per-module
      licence.
- [x] `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1) and `SECURITY.md` — the latter scoped to
      what maddi actually is: it parses untrusted input, and the IDE daemon listens locally.
- [x] Eight labels beyond the nine defaults: `front-end:javac`, `front-end:kotlin`,
      `engine:analyzer`, `engine:link`, `engine:prepwork`, `ide`, `build/ci`, `corpus`. No `docs`
      label — the default `documentation` already covers it.

### Phase 2 — the first ~15 issues, chosen to be landable by a stranger

Not the backlog — the self-contained items, most of them `good first issue`:

- [ ] The three `test*plugin/README.md` files whose instructions cannot be followed
      (`doc-audit-2026-07-30.md` §3.5).
- [ ] Acronym-expansion inconsistency in the manual and book intros (`landing-surface-checklist.md` §8).
- [ ] Social preview image, still unset (§2).
- [ ] Javadoc warnings on the otherwise clean CI run (§3).
- [ ] `eclipse-plugin-state.md` vs `maddi-eclipse/README.md` contradict each other — one is wrong
      (`doc-audit` §3.3).
- [ ] The four open release legs (§6): Gradle plugin → Plugin Portal, CLI zips → GitHub Release,
      Maven plugin untested against a real `mvn`, licence split stated in the 0.9.0 release notes.

### Phase 3 — roadmaps become tracking issues, not 49 issues

The two hardening documents stay documents. Each gets **one tracking issue** plus **one issue per
`H` item** (~10 total). Filing 49 fine-grained M/L items on a repository with zero issues buries the
ones a newcomer could act on. `ide-todo.md` §4's five bullets are individually issue-shaped; §§1–3
are design and stay prose.

- [ ] Tracking issue + H items for `prep-analyzer hardening.md`.
- [ ] Tracking issue + H items for `modification-link-analyzer hardening.md`.
- [ ] Five issues from `ide-todo.md` §4.
- [ ] Add a fourth status label to `docs/README.md`: **tracked** — the document explains, the issues
      track.

### Phase 4 — write the rule down

- [ ] `CONTRIBUTING.md`: a "Proposing a change" section — issue first for anything non-trivial, PR
      against the chosen branch, CI green, engine changes need the `FPDUMP` A/B, and which licence a
      contribution falls under per module.
- [ ] `AGENTS.md` §Working style: amend "new design notes go in `docs/`" to "design notes go in
      `docs/`; **open work items go to GitHub issues, not to new checkbox files**".
- [ ] `CLAUDE.md`: two lines pointing at the above; keep it short and pointer-based.

---

## 4. Decisions that are not mine to make

1. **PR target** — `main` with a regular `devel` → `main` merge, or `devel` as default. (§1)
2. **Contributor licensing — settle before the first PR, not after.** CodeLaser ships Refactor on
   this engine. Without a DCO or CLA, third-party contributions arrive LGPL-3.0-only from their
   authors: workable as things stand, but it forecloses any future relicensing or dual-licensing of
   the analyzer, and retrofitting agreement from contributors afterwards is the classic painful case.
   A DCO (a `Signed-off-by` line, enforced by an action) is the low-friction option and the
   recommendation; a CLA is heavier and deters casual contributors. Either way `CONTRIBUTING.md`
   must say plainly: contributions to `maddi-support` are Apache-2.0, everything else LGPL-3.0.
3. **Phase 3 granularity** — H items only (~10 issues), or all 49 checkboxes.

---

## 5. Customer-name scrub — done 2026-08-04

Found while auditing what an outsider would see: the repository named the customer behind the
private proving corpus — package names, class names, method names, business-domain vocabulary and
one verbatim line of their source, across documentation, production javadoc, comments and live test
fixtures.

Removed in two passes (84 occurrences of the customer/corpus names, then a second wave of ~20
type and method identifiers that contained neither word). The stand-ins are **`closed-core`** for
the corpus and **`com.example.*`** for its packages; see
[CONTRIBUTING.md §Names that must not appear](../CONTRIBUTING.md#names-that-must-not-appear).

A commit hook enforces it: `.githooks/` holds a scanner and `pre-commit` / `commit-msg` /
`pre-merge-commit` hooks. **The term list is not in the repository** — a list of the names to redact
is a decoder ring for the redaction — so the scanner reads it from `$MADDI_INTERNAL_NAMES`,
`git config maddi.internalNames`, or `~/.config/maddi/internal-names.txt`. **The hook must be enabled
per clone and per worktree** — `git config core.hooksPath .githooks` — which is the residual weakness,
together with `--no-verify` and fast-forward merges. A `--tracked` scan in `build.yml` is about six
lines and would make it a check that cannot be skipped.

- [~] A `guard` job in `build.yml` now fails if the term list is ever committed, and if the
      example file goes missing. The **scan itself** is not in CI: it needs the term list, which
      would mean putting the customer's name into GitHub Actions secrets. That is a deliberate
      open decision, not an oversight.

---

## 6. Not on this list

Outreach of any kind. The landing surface comes first — that is
[`landing-surface-checklist.md`](landing-surface-checklist.md), which this document does not
duplicate.
