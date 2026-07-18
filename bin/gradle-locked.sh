#!/bin/bash
# Serialize gradle invocations across concurrent AI threads / shells in this repo.
#
# WHY: a gradle build in one process rewrites build outputs (jars, class dirs) that another
# process's live test-fork JVMs are reading — javac on a half-written classpath entry fails with
# starImportScope NPEs, and JUnit discovery on a half-written class file produces garbled test
# names. Caught red-handed 2026-07-18 21:39:08 (jars rewritten mid-run, suite failing the same
# second). See maddi-modification-link/sv-remaining-catalogue.md, task #40.
#
# USE: bin/gradle-locked.sh <the usual gradlew arguments>
# The lock lives in build/ (git-ignored), is stale-proof (dead-pid detection), and waits — it
# never fails because someone else is building.
REPO="$(cd "$(dirname "$0")/.." && pwd)"
LOCKDIR="$REPO/build/maddi-gradle-lock.d"
mkdir -p "$REPO/build"
while ! mkdir "$LOCKDIR" 2>/dev/null; do
    OTHERPID=$(cat "$LOCKDIR/pid" 2>/dev/null)
    if [ -n "$OTHERPID" ] && ! kill -0 "$OTHERPID" 2>/dev/null; then
        # stale lock: owner is gone
        rm -rf "$LOCKDIR"
        continue
    fi
    sleep 5
done
echo $$ > "$LOCKDIR/pid"
trap 'rm -rf "$LOCKDIR"' EXIT INT TERM
cd "$REPO" && ./gradlew "$@"
