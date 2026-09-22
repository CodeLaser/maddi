#!/usr/bin/env python3
"""
Derive vavr's MAIN-ONLY input configuration from the one the maven-plugin route writes.

WHY THIS EXISTS
    vavr's test source set cannot be parsed from a configuration this route produces, and the way
    it fails is silent up to the point where it is fatal. vavr binds maven-clean-plugin:clean to
    `generate-sources` and does not set excludeDefaultDirectories, so every invocation that reaches
    that phase — the config route included — DELETES THE WHOLE OF vavr/target first (measured
    2026-09-21 with `mvn -pl vavr generate-test-sources -X`: "[INFO] Deleting .../vavr/target").
    The mojo therefore runs in a tree without target/generated-test-sources, does not record a
    directory that is not there, and writes a test source set missing its annotation-processor
    root — with nothing in the JSON to say so. The parse is where it lands: MatchTest.java imports
    io.vavr.MatchTest_DeveloperPatterns, javac cannot find it, maddi reports "Parser error(s)" and
    writes ZERO analysis results. The whole run is refused for a source set the analysis-hints
    campaign does not even want.

    ⛔ The same wipe means the BUILD PHASE MUST FOLLOW THE CONFIG PHASE for this entry, or the
    parse runs against a tree whose target/generated-sources/annotations is gone — one more main
    source root lost, and a missing root is not an error either.

    Main-only is also the right scope for this corpus: the subject is vavr's public API, and its
    tests would only add assertj/junit noise to every verdict.

    This is a DERIVATION, not a second capture: everything but the `test: true` source sets is
    copied through byte for byte, class path included. Re-run it after every config route run.

USAGE
    python3 vavr-main-config.py [<project-dir>]      # default: $TEST_OSS_ROOT/vavr, else ../../../test-oss/vavr
"""
import json
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent


def project_dir(argv):
    if len(argv) > 1:
        return Path(argv[1]).resolve()
    root = os.environ.get('TEST_OSS_ROOT')
    base = Path(root).resolve() if root else (HERE.parent.parent.parent / 'test-oss').resolve()
    return base / 'vavr'


def main(argv):
    d = project_dir(argv)
    src = d / 'inputConfiguration.json'
    dst = d / 'inputConfiguration.main.json'
    if not src.is_file():
        sys.exit(f"{src} does not exist — run the config phase first (`task corpus:config:vavr`)")

    cfg = json.loads(src.read_text())
    before = [s.get('name') for s in cfg.get('sourceSets') or []]
    cfg['sourceSets'] = [s for s in cfg.get('sourceSets') or [] if not s.get('test')]
    after = [s.get('name') for s in cfg['sourceSets']]
    if not after:
        sys.exit(f"{src}: every source set is a test source set — nothing to analyse")

    # The one thing worth asserting about the surviving set: the plugin sees vavr's two EXTRA main
    # roots only because the route names a phase (build-helper binds them to generate-sources).
    # Invoked as a bare mojo it writes one root per source set, which silently drops 30 of the ~100
    # main compilation units — src-gen/main/java is where Tuple0..Tuple8 and the API live.
    roots = cfg['sourceSets'][0].get('sourceDirectories') or []
    if len(roots) < 3:
        print(f"WARNING: {cfg['sourceSets'][0].get('name')} has {len(roots)} source root(s), expected 3 "
              f"(src/main/java, src-gen/main/java, target/generated-sources/annotations). The config "
              f"route was probably invoked without its `generate-test-sources` phase.", file=sys.stderr)

    dst.write_text(json.dumps(cfg, indent=2) + '\n')
    print(f"{dst}: kept {after}, dropped {[n for n in before if n not in after]}")


if __name__ == '__main__':
    main(sys.argv)
