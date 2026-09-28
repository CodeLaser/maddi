#!/usr/bin/env python3
"""
Derive Eclipse Collections' input configuration (API + implementation, both as SOURCE) from the two
configurations the maven-plugin route writes, one per module.

WHY THIS EXISTS
    The route writes one configuration per module. The implementation module's
    (eclipse-collections/target/inputConfiguration.json) sees its reactor sibling, the API, only as
    a class-path entry: 'org.eclipse.collections:eclipse-collections-api/classes', i.e.
    eclipse-collections-api/target/classes. Analysed that way, the API's ~250 object-typed interfaces
    (ImmutableList, RichIterable, ...) -- which carry many DEFAULT methods, and are the very types the
    analysis-hints campaign annotates -- would be byte code with no computed verdicts.

    So: the API's main source set (from eclipse-collections-api/target/inputConfiguration.json) is added
    as a second source set, the implementation's dependency on the API's classes is re-pointed to it,
    and the API's class-path entry is DROPPED. Keeping it would put the same types on the class path and
    in a source set, the shape of the 2026-08-12 sibling-jar shadowing (memory: maddi-classpath-jar-
    shadowing), where a reactor sibling was read as a library instead of as its source set.

    Test source sets are left out (the route writes none for these two modules: they have no tests;
    EC's tests live in the separate unit-tests module) -- the subject is the public API.

    Every other field is copied through from the implementation's configuration.

USAGE
    python3 eclipse-collections-config.py [--derive-only] [<project-dir>]
        # default: $TEST_OSS_ROOT/eclipse-collections, else ~/git/test-oss/eclipse-collections
    First CAPTURES both per-module configurations (the maven-plugin route, one mvn invocation over
    both modules; needs $MADDI_EXPORTS, see corpus/Taskfile.yml), then derives
    <project-dir>/inputConfiguration.json. --derive-only skips the capture.
    The capture reaches generate-test-sources and does NOT clean target/ (unlike vavr's), so the
    build may run before it; both orders were measured to leave target/classes in place.
"""
import json
import os
import subprocess
import sys
from pathlib import Path

API_CLASSES = 'org.eclipse.collections:eclipse-collections-api/classes'


def capture(project):
    exports = os.environ.get('MADDI_EXPORTS')
    if not exports:
        sys.exit('MADDI_EXPORTS is not set: without the javac --add-exports the mojo cannot load the '
                 'openjdk front-end (`task -d corpus env` prints it)')
    ver = os.environ.get('MADDI_PLUGIN_VERSION', '')
    cmd = ['mvn', '-B', '-pl', 'eclipse-collections-api,eclipse-collections', 'generate-test-sources',
           f'io.codelaser:maddi-mvnplugin:{ver}:write-input-configuration']
    env = dict(os.environ, MAVEN_OPTS=f'{exports} -Xmx6G')
    subprocess.run(cmd, cwd=project, env=env, check=True)


def main():
    args = [a for a in sys.argv[1:] if a != '--derive-only']
    if args:
        project = Path(args[0])
    else:
        root = os.environ.get('TEST_OSS_ROOT') or str(Path.home() / 'git' / 'test-oss')
        project = Path(os.path.expandvars(os.path.expanduser(root))) / 'eclipse-collections'
    if '--derive-only' not in sys.argv:
        capture(project)
    api = json.loads((project / 'eclipse-collections-api/target/inputConfiguration.json').read_text())
    impl = json.loads((project / 'eclipse-collections/target/inputConfiguration.json').read_text())

    api_main = [ss for ss in api['sourceSets'] if not ss.get('test')]
    impl_main = [ss for ss in impl['sourceSets'] if not ss.get('test')]
    if len(api_main) != 1 or len(impl_main) != 1:
        sys.exit(f'expected one main source set per module, got {len(api_main)} and {len(impl_main)}')
    api_ss, impl_ss = api_main[0], impl_main[0]

    if API_CLASSES not in impl_ss.get('dependencies', []):
        sys.exit(f'the implementation no longer depends on {API_CLASSES}: re-check this derivation')
    impl_ss['dependencies'] = [api_ss['name'] if d == API_CLASSES else d for d in impl_ss['dependencies']]

    before = len(impl['classPathParts'])
    impl['classPathParts'] = [c for c in impl['classPathParts'] if c.get('name') != API_CLASSES]
    if len(impl['classPathParts']) != before - 1:
        sys.exit(f'expected to drop exactly one class-path entry {API_CLASSES}')
    # the API's own class path is a subset of the implementation's (checked, not assumed)
    impl_cp = {c['name'] for c in impl['classPathParts']}
    missing = [c['name'] for c in api['classPathParts'] if c['name'] not in impl_cp]
    if missing:
        sys.exit(f'the API needs class-path entries the implementation does not have: {missing}')

    for ss in (api_ss, impl_ss):
        for d in ss['sourceDirectories']:
            if not Path(d).is_dir():
                sys.exit(f'source root {d} does not exist: build first (a missing root is not an error '
                         f'downstream, it silently contributes nothing)')

    impl['sourceSets'] = [api_ss, impl_ss]
    out = project / 'inputConfiguration.json'
    out.write_text(json.dumps(impl, indent=2) + '\n')
    print(f'wrote {out}: source sets {[ss["name"] for ss in impl["sourceSets"]]}, '
          f'{len(impl["classPathParts"])} class-path parts')


if __name__ == '__main__':
    main()
