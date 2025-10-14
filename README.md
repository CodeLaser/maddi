# maddi

_maddi_ stands for Modification Analyzer for Duplication Detection and Immutability.
It is a re-implementation of [e2immu](https://www.e2immu.org), an open source project which started in 2020 but has now been archived.

_maddi_ is and will remain open source. Please contact [Bart Naudts](mailto:bart.naudts@codelaser.io) at [CodeLaser](https://codelaser.io) for any information.

## Current status

_maddi_ is still under development, and, as of October 2025, will receive plenty of attention with the goal of being production ready in 2026.

- concepts: stable
- parser, resolver: has been tested on a number of open source projects, and one closed source 3M lines of code project. Not without errors, but pretty robust.
- modification analysis: has only been tested on one larger test set. Not ready for general use.
- plugins: Maven and Gradle plugins should work but have not received any attention in the last half year, since _maddi_ is mostly being run from [CodeLaser](https://codelaser.io)'s Refactor engine.

## Road to Immutability

This distribution contains the AsciiDoc sources for the [Road to Immutability](https://www.e2immu.org/docs/road-to-immutability.html), currently the only document detailing the concepts behind _maddi_.


## Building

_maddi_ can be built both with Gradle and Bazel:

- the Maven plugin is, naturally, built with Maven
- the Gradle plugin is, naturally, only built with Gradle
- quite a few tests do not run in Bazel, because they expect to find class files in some relative location. This needs to be fixed at some point.
- The Bazel build system has been added to test CodeLaser's Refactor input configuration construction system.

### Building with Gradle

Run `gradle test`

### Building with Bazel

Build all sub-projects with `bazel build //...`.

Run individual project's tests with ` bazel test //maddi-graph:maddi-graph_test`.

___

(C) Copyright Bart Naudts, 2020-2025.