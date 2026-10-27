# maddi 0.9.6

**maddi is now Apache-2.0, all of it.** Until 0.9.5 only the annotations were; the analyzer was LGPL-3.0-or-later.
From 0.9.6 the analyzer, the front ends, the translators, the build plugins, the IDE plugins and the command-line tools
are under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0), and CodeLaser BV holds the
copyright.

<!-- Draft. Add the release's other topics below the licence section, most important first. -->

## Licence: Apache-2.0 throughout

- **What changes.** Every module of [maddi](https://github.com/CodeLaser/maddi),
  [maddi-mod](https://github.com/CodeLaser/maddi-mod) and [maddi-dist](https://github.com/CodeLaser/maddi-dist) is
  licensed under Apache-2.0. Each repository has a `LICENSE` file in place of `COPYING` and `COPYING.LESSER`. The file
  headers, the published POMs, the VS Code extension's `package.json` and the Eclipse feature all say so.
- **The copyright holder is CodeLaser BV**, Belgium. The notices read `Copyright 2020-2026, CodeLaser BV`; until 0.9.5
  they named Bart Naudts, who develops maddi at CodeLaser.
- **Why.** Most people run maddi; they don't ship it. The LGPL's obligations only start when someone distributes the
  analyzer inside their own product, so in practice it protected nobody, while it did stop companies whose policies
  rule out anything GPL-family from building on maddi at all. maddi is also a library: its syntax tree, front ends
  and analysis engine are meant to be built upon. Apache-2.0 is the licence a Java shop already has on its approved
  list, and it carries an explicit patent grant.
- **What it means for you.**
  - *Using the annotations* (`maddi-annotation`, `maddi-support`): nothing changes. They have been Apache-2.0 since
    0.9.0.
  - *Running the analyzer* (CLI, Gradle or Maven plugin, IDE plugin): nothing changes in practice; the LGPL did not
    restrict that either.
  - *Building on maddi as a library, or embedding it in something you distribute*: you may now do so under the usual
    Apache-2.0 terms (keep the `NOTICE` and the licence text) and without the LGPL's obligations to provide maddi's
    source and to allow relinking.
- **Earlier versions keep their licence.** 0.9.5 and earlier were released under LGPL-3.0-or-later, and the copies
  already out there stay under those terms (the annotations from 0.9.0 onward excepted, which were Apache-2.0
  already). Apache-2.0 applies from 0.9.6. If you need the permissive terms for the analyzer, upgrade.
- **The Maven coordinates, package names and plugin ids do not change.**
- **For contributors: a CLA replaces the DCO.** Pull requests no longer need `Signed-off-by` lines. Instead you sign
  the [CodeLaser Contributor License Agreement](../../CLA.md) once, by replying to the bot on your first pull request.
  You keep the copyright in your contribution and CodeLaser BV receives a licence to it. One signature covers maddi,
  maddi-mod and maddi-dist.

## Java front end

- **A case label with several patterns keeps all of them.** Java 22 allows
  `case Circle _, Square _ ->` (JEP 456). Both front ends kept only the first pattern, so the analysis and the
  translators saw `case Circle _ ->` and nothing of the others. `SwitchEntry.otherPatterns()` now holds the rest
  ([#171](https://github.com/CodeLaser/maddi/issues/171), [#172](https://github.com/CodeLaser/maddi/pull/172)).

## Java → Kotlin translation

- **A record deconstruction becomes an `is` test, not the `else` arm.** `case Point(var x, var y) when x == y ->` used
  to print as an `else` branch with unbound variables. It now prints `is Point if o.x == o.y ->`. A nested pattern,
  `Box(String s)`, becomes a guard on the component (`is Box if o.content is String ->`) with a cast where the
  component is used. The same holds for `instanceof` with a record pattern. A case with several patterns and a guard
  has no Kotlin form; the printer reports it and keeps the Java.

## Installing

<!-- Copy from release-notes-0.9.5.md §Installing with the version bumped, once the artifacts are published. -->
