#!/usr/bin/env bash
#
# Build the two self-contained maddi CLI distributions and publish them as assets on a GitHub Release.
# This is the "command-line tools" leg of the publishing strategy (see PUBLISHING.md, Package 1, item 3).
#
#   maddi        (maddi-run-openjdk:distZip) — the openjdk (Java) runner
#   maddi-kotlin (maddi-run-kotlin:distZip)  — the mixed Java+Kotlin runner; the K2 'for-ide' jars ride
#                                              along in lib/, so this bundle is how Kotlin support ships
#
# Each bundle is self-contained: the launcher (bin/maddi[-kotlin]) has the javac --add-exports baked in
# and every runtime jar sits in lib/. No Maven resolution is involved on the consumer side.
#
# Usage:   ./release-cli.sh <tag>          e.g.  ./release-cli.sh v0.8.2
# Requires: an authenticated `gh` CLI (github.com/cli/cli) and a JDK on PATH.
#
set -euo pipefail

TAG="${1:-}"
if [[ -z "$TAG" ]]; then
    echo "usage: $0 <tag>   (e.g. v0.8.2)" >&2
    exit 2
fi

cd "$(dirname "$0")"

echo "==> Building the CLI distributions (version from gradle.properties)"
./gradlew :maddi-run-openjdk:distZip :maddi-run-kotlin:distZip

# Each zip lives in its own module's distributions dir, so these globs cannot cross-match.
OPENJDK_ZIP=$(ls maddi-run-openjdk/build/distributions/maddi-*.zip)
KOTLIN_ZIP=$(ls maddi-run-kotlin/build/distributions/maddi-kotlin-*.zip)
echo "    openjdk runner: $OPENJDK_ZIP"
echo "    kotlin  runner: $KOTLIN_ZIP"

if gh release view "$TAG" >/dev/null 2>&1; then
    echo "==> Release $TAG already exists; uploading assets (--clobber overwrites same-named assets)"
else
    echo "==> Creating release $TAG"
    gh release create "$TAG" --title "$TAG" --notes "maddi $TAG — command-line distributions."
fi

gh release upload "$TAG" "$OPENJDK_ZIP" "$KOTLIN_ZIP" --clobber
echo "==> Done. Both CLI zips attached to release $TAG."
