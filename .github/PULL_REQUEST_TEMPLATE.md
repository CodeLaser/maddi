<!--
Thanks for contributing. New here? CONTRIBUTING.md covers the build, the fast/slow test
split, and the rules that are easy to violate and expensive to learn.
-->

## What this changes, and why

<!-- One paragraph. If it fixes an issue, link it. -->

## Checks

- [ ] `./gradlew build` passes (compile + everything not tagged `slow`).
- [ ] New or changed behaviour has a test. For link-engine behaviour, extend
      `TestLinkMethodCall` and friends rather than asserting ad hoc elsewhere.
- [ ] Any test that parses a real-world corpus is tagged `@Tag("slow")`.
- [ ] Every commit is signed off (`git commit -s`) — the [DCO](../DCO). A CI job checks this.

## If this touches the engine

Performance or engine-structure changes are accepted only with a **byte-identical `FPDUMP` A/B
comparison** on the proving-ground corpora (modulo the documented constructor non-confluence).
Speed never buys verdict changes. See CONTRIBUTING.md §"Rules that are easy to violate".

- [ ] Not an engine change — skip this section.
- [ ] `FPDUMP` A/B run, byte-identical. Corpora used: <!-- e.g. guava, timefold -->
- [ ] Verdicts *do* change, deliberately. What changed and why:

<!--
A green slowTest is not by itself evidence anything ran: it can be cached, skipped on an
absent corpus, vacuous, or heap-starved. AGENTS.md §Commands lists the four things to check.
-->

## If this changes what the analyzer means

- [ ] Not a concept change — skip.
- [ ] `road-to-immutability/llm-summary.md` updated in this same PR (and the book chapter if
      affected). The summary is the authoritative condensed vocabulary; code and prose must agree.

## Licence

maddi is **LGPL-3.0**, except `maddi-support` (the annotations) which is **Apache-2.0** from
0.9.0 onward. By signing off you contribute under the licence of the module you touched. There is
no CLA and no copyright assignment.
