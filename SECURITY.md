# Security policy

## Reporting

Mail **bart.naudts@codelaser.io**, or open a
[private advisory](https://github.com/CodeLaser/maddi/security/advisories/new). Please do not
open a public issue for something exploitable.

You will get an acknowledgement within a few working days. maddi is developed by one person, so
please read the scope below before assuming urgency — most of it does not apply.

## What maddi is, and what that means for its attack surface

maddi is a **static analyzer**: a developer tool that reads source and class files and reports
annotations. It opens no ports, serves no requests, and stores no credentials. There is no
deployment to compromise.

That said, it does two things worth taking seriously:

- **It parses untrusted input.** Running maddi over a hostile codebase, jar or build log should
  not do anything worse than fail. A crash is a bug; anything that reads or writes outside the
  configured input and output directories, or executes code from the analyzed project, is a
  security issue — report it privately.
- **The IDE daemon listens locally.** The `maddi-ide-daemon` process serves the IntelliJ,
  Eclipse and VS Code front ends. It is intended for local use by the developer running it.
  A way to reach it from off-machine, or to make it act on input from another user on the same
  machine, is in scope.

Out of scope: findings that require an attacker who already runs code as you, and reports about
the *analyzed* project's vulnerabilities rather than maddi's own.

## Supported versions

maddi is **not yet production ready** and has no release branches. Fixes land on `main`; the
annotations artifact (`io.codelaser:maddi-support`) is versioned on Maven Central. Please report
against `main` or the latest published version.
