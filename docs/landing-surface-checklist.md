# Landing surface — checklist

**Status: plan.** Written 2026-07-22. Everything an interested outsider hits *before* they read any
code, and what is currently missing from it. Ordered by leverage, not by effort.

The diagnosis this came from: maddi has three public properties (`github.com/CodeLaser/maddi`,
`e2immu.org`, `codelaser.io`) with **no links between them**, no released artifact, and no CI. A
successful launch today would send interested people to a repo they cannot install from. Fix the
landing surface before doing any outreach — discovery without a place to land wastes the one shot.

## 1. e2immu.org — the highest-leverage item

It is the only property with existing search traffic and inbound links, it is live, and it does not
mention maddi anywhere. It still advertises e2immu v0.6.2 as if current.

**Edited locally in `~/git/old/e2immu-site` on 2026-07-22, built and verified, NOT deployed.**
27 of 27 generated pages now carry the notice, the book and manual included.

Changes made:

- `config/_default/params.toml` — `alert = true` + `alertText` (Doks' built-in bottom bar).
- `layouts/_default/baseof.html` — **new local override** of the theme file. Doks gates the alert on
  `{{ if and .IsHome … }}`, i.e. home page only; the override drops `.IsHome` so docs and blog pages
  (what inbound links actually point at) get it too.
- `content/_index.md` + `layouts/index.html` — hero now leads with a "Continue to maddi" button,
  demoting the e2immu docs to a secondary button.
- `static/docs/road-to-immutability.html`, `static/docs/manual.html` — **these are static files**,
  generated AsciiDoc that Hugo copies verbatim, so no layout ever applies to them. The banner is
  injected directly after `<body>`. The book is the single most-linked page on the site; it would
  otherwise have been the one page to miss the notice.
- `content/blog/{dataflowanalysis,controlflowanalysis}/index.md` — removed a duplicated `date:`
  front-matter key (see toolchain note below).

### The toolchain is fragile — resolve before deploying

- The site builds **only with the vendored `node_modules/.bin/hugo`, v0.82.1** (2021). Current Hugo
  (0.164) fails: first on the duplicate `date:` keys (now fixed), then on the Doks SCSS
  (`Undefined variable: "$font-size-md"` in `_global.scss`) under modern Dart Sass. Fixing that
  means upgrading or vendoring the theme — real work, and not a prerequisite for the banner.
### How publishing actually works (verified 2026-07-22)

The live chain: `www.e2immu.org` → CNAME `e2immu.github.io` → repo **`e2immu/e2immu.github.io`**,
GitHub Pages serving branch `master`, path `/`. The site is committed HTML; nothing builds server
side. (The local `public/` remote reads `bnaudts/bnaudts.github.io`, but that repo was
renamed/transferred — GitHub redirects it to `e2immu/e2immu.github.io`. Both routes converge.)

There are three publishing mechanisms, and **two of the three are dead**:

1. `.github/workflows/deploy-site.yml` — fires on push to `main`, builds with npm+hugo, clones
   `e2immu/e2immu.github.io` using a `GTOKEN` secret and pushes the built site. **Dead:** pinned to
   `runs-on: ubuntu-18.04`, a runner GitHub retired. A push queues a job that never gets a runner.
   *Historically this is why `git commit && git push` on the source repo was all that was needed.*
2. `netlify.toml` — pins `NODE_VERSION = "15.5.1"`, builds via `npx hugo`. **Assume dead**; Netlify
   is very unlikely to still provision Node 15.
3. `deploy.sh` — runs `hugo`, then commits and pushes the nested `public/` repo. **Broken as
   written:** it calls bare `hugo`, which now resolves to the Homebrew build and dies on the SCSS.

So the working manual deploy is: build with the **vendored** binary, then push `public/`.

```bash
cd ~/git/old/e2immu-site
./node_modules/.bin/hugo                    # NOT ./deploy.sh
cd public && git add -A && git commit -m "…" && git push origin master
```

Local `public/` was verified in sync with the live branch (0 ahead, 0 behind), so this is a clean
fast-forward, not a clobber.

- [ ] Deploy deliberately with the three commands above. **Not run — it publishes to the live site.**
- [ ] Delete `.github/workflows/deploy-site.yml` rather than repairing it. It holds a `GTOKEN`
      secret and runs `npm install` against a 2021 lockfile; revived on a modern runner, that
      executes five-year-old install scripts with a push token in the environment. The site is
      being wound down — remove the automation instead of modernising it.
- [ ] Same reasoning for the Dependabot alerts on that repo: all 25 npm packages are
      `devDependencies` (`dependencies` is empty) and the published artifact is static HTML, so
      nothing shipped to a visitor is affected. Dismiss them or switch alerts off for the repo
      rather than spending effort on an archived toolchain.
- [ ] Keep the old content reachable. Do not 301 the whole site; the archived pages are what the
      backlinks point at, and breaking them loses the traffic you are trying to redirect.
- [ ] Longer term, replace the injected static banner by re-rendering the book from maddi's own
      `road-to-immutability/` sources (§4), so the notice lives in the source rather than in a
      patched artifact.

### The README worked example is verified, not asserted

The `Config` example in the README was run through the analyzer on 2026-07-22 (commit `c20ab60b`),
not reasoned out from the rules. Verdict fingerprint:

```
{field.unmodified=true=2, method.nonModifying=false=2, method.nonModifying=true=2,
 type.immutable=@FinalFields=1, type.immutable=@Immutable(hc=true)=1}
```

The discriminator is at **field** level, not the constructor: storing the parameter gives
`parameter ≡ field` on `java.util.Map`'s `§m` (the modification component — one mutable thing, two
names), while `Map.copyOf` leaves only `⊇` on `§$$s` (the elements). Both types are `@Container`
either way; what the copy buys is `immutableType` 1→2 plus `independentType` / `immutableField` /
`independentField`. Reproduction script and expected output are in the session scratchpad
(`validate/run.sh`); worth promoting into a committed demo (§4).

Two gotchas that cost a run each, both worth knowing before writing docs that quote numbers:
preloading `libs/test` analysis hints aborts unless junit-jupiter-api is on the classpath (use
`jdk` alone for small examples), and piping gradle through `tail` makes `$?` the exit status of
`tail`, so a failed analysis reads as success.

## 2. GitHub repo metadata — DONE 2026-07-22

The repo previously had no topics at all, so it was invisible to GitHub's own topic search, and no
homepage link. Now:

- **Description**: "Whole-program static analyzer that computes immutability, modification and
  independence for Java and Kotlin." (drops the acronym expansion, per §8)
- **Homepage**: `https://www.e2immu.org/docs/road-to-immutability.html` — the book. Revisit when
  the book gets its own home under a maddi domain (§4).
- **Topics**: `annotations`, `dataflow-analysis`, `immutability`, `java`, `javac`, `kotlin`,
  `program-analysis`, `static-analysis`.
- **Discussions**: enabled — questions about the *concepts* previously had nowhere to go.

> **Tooling note.** The `gh` CLI on this machine is authenticated as `bart-naudts_soficonv`, which
> has `{admin: false, push: false, pull: true}` on `CodeLaser/maddi`. Any write through `gh` fails
> with a bare **HTTP 404** (GitHub returns 404, not 403, for repos you can read but not administer).
> Git pushes still work because the SSH remote uses a different, privileged identity. The same
> account limit is why Dependabot alerts could not be read in §1. Do repo administration in the web
> UI, or `gh auth login` as the owning account first.

- [ ] Social preview image — still unset. It is what renders whenever anyone links the repo in
      Slack, on Mastodon/X, or in a chat client; the default is a grey placeholder.

## 3. CI — workflow added 2026-07-22, first run not yet observed

For a static-analysis tool, an unproven build is a credibility problem before anyone reads a line.
`.github/workflows/build.yml` runs `./gradlew build` on push to `main`, on pull requests, and on
manual dispatch: JDK 26 via `actions/setup-java` (Temurin `jdk-26.0.1+8` is GA for linux/x64 and
matches the development JDK), Gradle caching via `gradle/actions/setup-gradle@v4`, test reports
uploaded as an artifact on failure, and a concurrency group so a newer push cancels an in-flight run.

Two things deliberately NOT done:

- **`slowTest` is not in CI, and should not be added.** The large-corpus tests need external corpora
  (`test-oss`, `testarchive`) that do not exist on a runner; without them every test skips via a
  JUnit assumption and the job reports a green that proves nothing — the exact failure mode
  `AGENTS.md` §Commands warns about. If it is ever wanted, it needs the corpora provisioned *and* a
  roll-call assertion, not just the task name.
- **No README badge yet.** Add it only after the first genuinely green run; a red badge is worse
  than no badge. The line to add under the title:
  `![build](https://github.com/CodeLaser/maddi/actions/workflows/build.yml/badge.svg)`

- [ ] Watch the first run. The known risk is `maddi-intellij`, which resolves
      `intellijIdea("2025.3")` — a large IDE distribution downloaded on a cold Gradle cache. If it
      makes runs slow or flaky, exclude that project from the CI invocation rather than dropping
      the workflow; the analyzer stack is what outsiders evaluate.
- [ ] No Gradle toolchain is declared anywhere, so the build silently uses whatever `JAVA_HOME`
      offers. Declaring one would make CI and local builds agree by construction instead of by
      convention.

## 4. Publish the book

*The Road to Immutability* is six years of conceptual work and is the strongest single asset the
project has. It is the primary entry point, not an appendix to the tool — it stands alone as an
educational artifact for readers who never run the analyzer.

**Re-rendered from maddi's own sources on 2026-07-22** (`./gradlew :road-to-immutability:buildDocs`)
and installed into the site, replacing the 2021 edition. Built, banner re-injected, verified in the
Hugo output — awaiting the same deploy as §1.

What the 2021 edition on the live site was missing:

- **Ch. 10 "The link system"** and **Ch. 11 "Convergence: the iterating analyzer"** — the two
  technical chapters (sources `105-link-system`, `108-convergence`) did not exist in 2021 at all.
- A much-expanded immutability chapter: *Ignoring modifications as manual hidden content*, *The
  confinement guard*, *The stratum boundary by example*, *Static side effects are the global-escape
  arm*, *Value-based classes*, *Dynamic type annotations*.
- Eventual immutability restructured into Builders / Definition / Propagation / Before the mark.
- Drops the two pre-sv chapters that are excluded from `index.adoc` — *Further notes on
  immutability* (090) and *Preconditions and instance state* (100) — whose terminology no longer
  matches the engine. Publishing the old edition was actively teaching retired vocabulary.

**Fixed 2026-07-22:** `115-use-in-analyzer.adoc` opened with `==` where every other section file
uses `=`. With `leveloffset=+1` on the include, that demoted it to a subsection of ch. 12 instead of
a chapter of its own. Now *13. Support classes in the analyzer*, with *Other annotations*
renumbered to 14. The `[#in-the-analyzer]` anchor is unchanged, so the two cross-references from
`060-eventual.adoc` still resolve (5 `href="#in-the-analyzer"` links in the built HTML).

- [ ] Decide the same question for `static/docs/manual.html`, still the 2021 e2immu manual. Unlike
      the book, the manual documents *operating* a tool — and maddi has no release yet (§6), so a
      fresh render would describe CLI and plugins nobody can install. Either rebuild it
      (`./gradlew :maddi-manual:buildDocs`) at release time, or leave the old one carrying its
      archive banner until then.
- [ ] Link the book from the repo homepage field (§2), the README, and codelaser.io.
- [ ] Longer term the book wants its own stable home under a maddi domain; hosting the current
      edition on the archived predecessor's site is a stopgap, not the destination.

## 5. codelaser.io

Says nothing about maddi. The OSS/commercial relationship is better stated plainly than left to be
inferred — unclear boundaries make people suspicious of exactly the projects they would otherwise
adopt.

- [ ] One paragraph: maddi is the LGPL analysis engine, Refactor is the commercial product built on
      it, maddi stays open source. The README now carries the mirror image of this.

## 6. Release 0.9.0

`PUBLISHING.md` reports the wiring as essentially complete — what remains is credentials and
deliberate outward-facing runs. Until this happens, "install maddi" has no answer.

- [ ] `maddi-support` → Maven Central (jreleaser; appendix in `PUBLISHING.md`).
- [ ] Gradle plugin → Plugin Portal (needs `com.gradle.plugin-publish` applied + a Portal key).
- [ ] CLI zips → GitHub Release via `release-cli.sh <tag>` (needs authenticated `gh`).
- [ ] Maven plugin — untested against a real `mvn` invocation. Either test it or say so in the
      release notes.
- [ ] Flip the README "Try it" section from build-from-source to download once this lands.

## 7. Split the licence — DECIDED 2026-07-22

The **analyzer stays LGPL-3.0**. **`maddi-support` moves to a permissive licence** (Apache-2.0 or
BSD): it is the jar users compile their own code against, and LGPL on an annotations dependency is
the kind of thing corporate legal departments stall on for months. Do this *before* the release —
cheap now while nothing is published, expensive once it is on Central and third parties depend on it.

- [ ] Pick Apache-2.0 or BSD (Apache-2.0 is the more common expectation for a Java annotations
      artifact, and carries an explicit patent grant).
- [ ] Add the licence file under `maddi-support/`, and make clear it governs that module only.
- [ ] Update the `pom` metadata in the `maddi-support` publication block — Maven Central shows the
      POM licence, and it must not say LGPL.
- [ ] Check the source headers: `copyright_file.txt` at the repo root drives header insertion.
- [ ] Say the split explicitly in the release notes; a project that quietly relicenses part of
      itself invites exactly the suspicion the split is meant to remove.

## 8. Naming — DECIDED 2026-07-22

Keep **maddi**; it pronounces better than *e2immu*, which reads as technical and difficult.
Stop leading with the acronym — "duplication detection" appears nowhere else in the documentation,
so the first sentence a newcomer reads advertises a capability they will never find. The README no
longer expands it at all.

- [ ] Two other places still lead with the expansion, if you want them consistent:
      `maddi-manual/src/docs/asciidoc/sections/010-overview.adoc` (first line) and
      `road-to-immutability/src/docs/asciidoc/sections/010-introduction.adoc` (paragraph 1).

## Not on this list

Outreach — conference CFPs, academic venues, direct contact with adjacent projects, Show HN. All of
it depends on §1–§4 being done first.
