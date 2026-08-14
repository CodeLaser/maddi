#!/usr/bin/env python3
"""
The corpus catalogue: read the per-corpus YAML entries and act on them.

WHY THIS EXISTS
    Every fact about a corpus used to live in a task name, a `desc:` string or a comment — so
    nothing could ask "which corpora need a non-default JDK", "which are active", or "did this
    build produce what the next phase needs". The catalogue makes those queryable, and this script
    is the only thing that reads it.

THE IDEA IT IS BUILT ON
    Success is defined by what the NEXT PHASE needs, not by the command's exit code. A build is
    good if `build.provides` exists afterwards, whatever Maven returned. That is what makes a
    partially-building project (langchain4j, hive) an ordinary entry rather than an exception.

CATALOGUE DIRECTORIES
    $CORPUS_CATALOGUE is a colon-separated list; later directories win on a name collision, so a
    private overlay can add entries (and override one) without this repo knowing it exists.
    Default: the catalogue/ next to this script's parent.

    catalogue.py list   [--status active]     one line per entry
    catalogue.py show   <name>                the resolved entry, as parsed
    catalogue.py doctor [<name>...]           obtained? built? configured? per corpus
    catalogue.py plan   <phase> <name>        print the shell command a phase would run
    catalogue.py check-provides <name>        assert build.provides exists  (exit 1 if not)
    catalogue.py check-jdk <name>             assert build.jdk is satisfied (exit 1 if not)
    catalogue.py baseline <name> [--record]   source-set inventory: diff against the recorded one

`plan` prints rather than executes: Task runs the command so that its output streams and its exit
code is Task's, and so that `--dry` shows something real.
"""
import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

# PyYAML when it is importable, the strict stdlib subset reader otherwise. Nothing else in this
# repo's scripts needs a third-party package, and on a PEP 668 machine `pip3 install pyyaml` simply
# fails -- so requiring it would put an install step in front of the catalogue we moved here
# precisely so contributors could use it. See miniyaml.py.
sys.path.insert(0, str(Path(__file__).resolve().parent))
try:
    import yaml as _yaml

    def parse_yaml(text, name):
        return _yaml.safe_load(text)

    def dump_yaml(obj):
        return _yaml.safe_dump(obj, sort_keys=False, allow_unicode=True)
except ImportError:
    import miniyaml

    def parse_yaml(text, name):
        return miniyaml.load(text, name)

    def dump_yaml(obj):
        return json.dumps(obj, indent=2, ensure_ascii=False)   # readable enough for `show`

HERE = Path(__file__).resolve().parent
DEFAULT_CATALOGUE = HERE.parent / 'catalogue'
PHASES = ('build', 'config', 'parse', 'tests')


# ---------------------------------------------------------------- loading

def catalogue_dirs():
    raw = os.environ.get('CORPUS_CATALOGUE') or str(DEFAULT_CATALOGUE)
    return [Path(p).expanduser() for p in raw.split(':') if p]


def load_all():
    """-> {name: entry}. Later catalogue dirs override earlier ones, by name."""
    out = {}
    for d in catalogue_dirs():
        if not d.is_dir():
            continue
        for f in sorted(d.glob('*.yml')) + sorted(d.glob('*.yaml')):
            e = parse_yaml(f.read_text(), str(f)) or {}
            e.setdefault('name', f.stem)
            e['_file'] = str(f)
            out[e['name']] = e
    return out


def load_one(name):
    all_ = load_all()
    if name not in all_:
        sys.exit(f"no catalogue entry '{name}' in {':'.join(str(d) for d in catalogue_dirs())}")
    return all_[name]


def oss_root():
    return Path(os.environ.get('TEST_OSS_ROOT') or (Path.home() / 'git' / 'test-oss')).expanduser()


def project_dir(entry):
    return oss_root() / (entry.get('dir') or entry['name'])


# ---------------------------------------------------------------- state

def state(entry):
    """What is true on THIS machine — always computed, never stored in the entry."""
    d = project_dir(entry)
    provides = (entry.get('build') or {}).get('provides') or []
    return {
        'present': d.is_dir(),
        'built': bool(provides) and all((d / p).exists() for p in provides),
        'configured': (d / 'inputConfiguration.json').is_file(),
        'buildable': bool((entry.get('build') or {}).get('cmd')),
        'obtainable': (entry.get('source') or {}).get('kind') in ('git',),
    }


def source_sets(entry):
    """-> [(name, is_test)] from the generated config, or [] when there is none."""
    f = project_dir(entry) / 'inputConfiguration.json'
    if not f.is_file():
        return []
    try:
        ss = json.loads(f.read_text()).get('sourceSets') or []
    except Exception:
        return []
    out = []
    for s in ss:
        n = s.get('name') or '?'
        out.append((n, 'test' in n.rsplit('/', 1)[-1].lower()))
    return out


# ---------------------------------------------------------------- phases

def _mvn_exclusions(cfg):
    ex = cfg.get('exclude_modules') or []
    return f" -pl '{','.join(ex)}'" if ex else ''


def plan(entry, phase):
    """-> the shell command for one phase, or None when the entry does not define it."""
    name = entry['name']
    d = project_dir(entry)

    if phase == 'build':
        b = entry.get('build') or {}
        return b.get('cmd')

    if phase == 'config':
        c = entry.get('config') or {}
        route = c.get('route')
        if not route or route == 'none':
            return None
        maddi = Path(os.environ.get('MADDI_REPO') or HERE.parent.parent).resolve()
        out = d / 'inputConfiguration.json'
        if route == 'maven-plugin':
            ver = os.environ.get('MADDI_PLUGIN_VERSION', '')
            return (f'MAVEN_OPTS="$MADDI_EXPORTS -Xmx{c.get("mem", "6G")}" '
                    f'mvn -pl {c["module"]} generate-test-sources '
                    f'io.codelaser:maddi-mvnplugin:{ver}:write-input-configuration'
                    f' && cp {c["module"]}/target/inputConfiguration.json {out}')
        if route == 'maven-log':
            jh = f'JAVA_HOME={c["build_java_home"]} ' if c.get('build_java_home') else ''
            # `clean` is mandatory: maven-compiler-plugin skips an up-to-date module and a skipped
            # module emits no "Command line options:" line at all, so capturing over an already
            # built reactor yields a SILENTLY PARTIAL config -- measured on timefold, 22 source
            # sets instead of 65, missing core/main. Nothing downstream reveals the loss.
            return (f'{jh}MAVEN_OPTS="$MADDI_EXPORTS -Xmx{c.get("mem", "6G")}" '
                    f'./mvnw -X clean {c["tasks"]}{_mvn_exclusions(c)} > compile.log 2>&1; '
                    # Filter BEFORE maddi reads it: ParseJavacList does readString on the whole
                    # file, and a >2GB log dies on the JVM's max array size, which no -Xmx fixes.
                    # Equivalent input, not a shortcut -- these are exactly the lines it keeps.
                    f"grep -aE '^\\[DEBUG] -d ' compile.log > compile.javac.log && "
                    f'{maddi}/gradlew -p {maddi} :maddi-run-openjdk:run '
                    f'--args="--compile-log {d}/compile.javac.log --write-input-configuration {out}"')
        if route in ('gradle-log', 'gradle-log-kotlin'):
            target = 'maddi-run-kotlin' if route.endswith('kotlin') else 'maddi-run-openjdk'
            grep = ("grep -aE 'Compiler arguments:|\\[KOTLIN] compiler arguments:'"
                    if route.endswith('kotlin') else "grep -a 'Compiler arguments:'")
            extra = ' --no-configuration-cache -Dorg.gradle.warning.mode=summary' if route.endswith('kotlin') else ''
            return (f'./gradlew --no-build-cache --rerun-tasks {c["tasks"]}{extra} --debug 2>&1 | '
                    f'{grep} > compile.log; '
                    f'{maddi}/gradlew -p {maddi} :{target}:run '
                    f'--args="--compile-log {d}/compile.log --write-input-configuration {out}"')
        if route == 'script':
            return f'python3 {HERE / Path(c["script"]).name}'
        sys.exit(f'{name}: unknown config.route {route!r}')

    if phase == 'parse':
        p = entry.get('parse') or {}
        if not p.get('test'):
            return None
        maddi = Path(os.environ.get('MADDI_REPO') or HERE.parent.parent).resolve()
        mod = {'openjdk': 'maddi-run-openjdk', 'kotlin': 'maddi-run-kotlin',
               'main': 'maddi-run-main'}[p.get('runner', 'openjdk')]
        return (f'{maddi}/gradlew -p {maddi} :{mod}:slowTest '
                f"--tests '*{p['test']}' --rerun-tasks")

    if phase == 'tests':
        return (entry.get('tests') or {}).get('cmd')

    sys.exit(f'unknown phase {phase!r}')


# ---------------------------------------------------------------- checks

def check_provides(entry):
    b = entry.get('build') or {}
    provides = b.get('provides') or []
    if not provides:
        print(f"{entry['name']}: no build.provides declared — nothing to assert", file=sys.stderr)
        return 0
    d = project_dir(entry)
    missing = [p for p in provides if not (d / p).exists()]
    for p in missing:
        print(f'MISSING {d / p}', file=sys.stderr)
    if missing:
        exp = b.get('expect', 'complete')
        print(f"{entry['name']}: build did NOT provide {len(missing)}/{len(provides)} required "
              f"path(s) (build.expect={exp})", file=sys.stderr)
        return 1
    print(f"{entry['name']}: all {len(provides)} required path(s) present", file=sys.stderr)
    return 0


def check_jdk(entry):
    req = (entry.get('build') or {}).get('jdk')
    if not req:
        return 0
    home = os.environ.get('BUILD_JAVA_HOME') or os.environ.get('JAVA_HOME')
    if not home:
        print(f"{entry['name']}: needs JDK {req} but neither BUILD_JAVA_HOME nor JAVA_HOME is set",
              file=sys.stderr)
        return 1
    try:
        out = subprocess.run([str(Path(home) / 'bin' / 'java'), '-XshowSettings:properties',
                             '-version'], capture_output=True, text=True, timeout=30)
        props = out.stderr + out.stdout
    except Exception as e:
        print(f"{entry['name']}: cannot run {home}/bin/java: {e}", file=sys.stderr)
        return 1
    ok = True
    want_v = req.get('version')
    if want_v is not None:
        m = re.search(r'java\.specification\.version = (\d+)', props)
        got = m.group(1) if m else '?'
        if str(want_v) != got:
            print(f"{entry['name']}: needs JDK {want_v}, {home} is {got}", file=sys.stderr)
            ok = False
    want_vendor = req.get('vendor')
    if want_vendor:
        m = re.search(r'java\.vendor = (.+)', props)
        got = (m.group(1).strip() if m else '?')
        if not any(v.lower() in got.lower() for v in want_vendor):
            # trino's maven-enforcer rejects by VENDOR, not version, and fails 30s in with a stack
            # trace rather than anything readable. Fail here instead, with the fix in the message.
            print(f"{entry['name']}: needs vendor {want_vendor}, {home} is {got!r}", file=sys.stderr)
            ok = False
    return 0 if ok else 1


# ---------------------------------------------------------------- baseline

_TYPES_RE = re.compile(r'^\s*(?P<set>\S+?)\s*:\s*(?P<n>\d+)\s+primary types?\s*$', re.M)


def baseline_path(entry):
    b = (entry.get('config') or {}).get('baseline')
    if not b:
        return None
    return (Path(entry['_file']).parent / b).resolve()


def baseline_cmd(entry, record):
    """The SOURCE-SET INVENTORY of the generated config: name + whether it is a test source set.

    Recorded, never hand-written -- pulsar has 90 source sets and timefold 65, and a table nobody
    maintains is worse than no table.

    ⚠ This is NOT yet the per-source-set PRIMARY TYPE count the design asks for. maddi logs only a
    total ("Running prep analyzer on {} types"), so per-source-set counts need a summary output on
    the maddi side first. What this does catch is the failure that has actually bitten: a silently
    partial config. Capturing timefold's compile log without `clean` yields 22 source sets instead
    of 65, missing core/main, and it loads and analyses without complaint -- invisible to every
    other instrument. A drift in this inventory is that failure, named.
    """
    p = baseline_path(entry)
    if not p:
        print(f"{entry['name']}: no parse.baseline declared", file=sys.stderr)
        return 1
    sets = source_sets(entry)
    if not sets:
        print(f"{entry['name']}: no config on disk — run the config phase first", file=sys.stderr)
        return 1
    if record:
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text('# source set\tis_test — recorded by `catalogue.py baseline --record`\n'
                     + ''.join(f'{n}\t{int(t)}\n' for n, t in sorted(sets)))
        print(f'recorded {len(sets)} source sets -> {p}', file=sys.stderr)
        return 0
    if not p.is_file():
        print(f'{p} does not exist — record it with RECORD=1', file=sys.stderr)
        return 1
    want = {}
    for line in p.read_text().splitlines():
        if line.strip() and not line.startswith('#'):
            k, _, v = line.partition('\t')
            want[k] = v
    got = {n: str(int(t)) for n, t in sets}
    added = sorted(set(got) - set(want))
    removed = sorted(set(want) - set(got))
    changed = sorted(k for k in set(want) & set(got) if want[k] != got[k])
    for k in removed:
        print(f'- {k}', file=sys.stderr)
    for k in added:
        print(f'+ {k}', file=sys.stderr)
    for k in changed:
        print(f'~ {k}: main<->test changed', file=sys.stderr)
    if added or removed or changed:
        print(f"{entry['name']}: source sets drifted ({len(want)} -> {len(got)}). "
              f'Read the diff, then accept it with RECORD=1.', file=sys.stderr)
        return 1
    print(f"{entry['name']}: {len(got)} source sets, unchanged", file=sys.stderr)
    return 0


# ---------------------------------------------------------------- commands

def cmd_list(args):
    for name, e in sorted(load_all().items()):
        if args.status and e.get('status') != args.status:
            continue
        s = state(e)
        flags = ''.join(c if v else '-' for c, v in
                        (('P', s['present']), ('B', s['built']), ('C', s['configured'])))
        print(f"{name:22} {e.get('status', '?'):10} {flags}  {e.get('summary', '')[:60]}")


def cmd_show(args):
    e = load_one(args.name)
    print(dump_yaml({k: v for k, v in e.items() if k != '_file'}))
    sets = source_sets(e)
    if sets:
        print(f'# source sets on disk: {len(sets)} '
              f'({sum(1 for _, t in sets if not t)} main, {sum(1 for _, t in sets if t)} test)')


def cmd_doctor(args):
    all_ = load_all()
    names = args.names or sorted(all_)
    print(f"{'corpus':22} {'status':10} {'present':>8} {'built':>7} {'config':>7}  notes")
    print('-' * 86)
    rc = 0
    for n in names:
        e = all_[n] if n in all_ else None
        if e is None:
            print(f'{n:22} NOT IN CATALOGUE')
            rc = 1
            continue
        s = state(e)
        notes = []
        if not s['present']:
            notes.append('clone it' if s['obtainable'] else 'COPY-ONLY: rsync from a machine that has it')
        elif s['buildable'] and not s['built']:
            notes.append('build incomplete: build.provides missing')
        elif not s['configured'] and (e.get('config') or {}).get('route') not in (None, 'none'):
            notes.append('no inputConfiguration.json')
        if s['present'] and s['configured']:
            sets = source_sets(e)
            t = sum(1 for _, is_t in sets if is_t)
            notes.append(f'{len(sets)} source sets, {t} test')
            if not t and (e.get('tests') or {}).get('cmd'):
                notes.append('!! tests.cmd declared but config has NO test source sets')
                rc = 1
        print(f"{n:22} {e.get('status', '?'):10} {str(s['present']):>8} {str(s['built']):>7} "
              f"{str(s['configured']):>7}  {'; '.join(notes)}")
    return rc


def cmd_plan(args):
    c = plan(load_one(args.name), args.phase)
    if not c:
        print(f'{args.name}: no {args.phase} phase defined', file=sys.stderr)
        return 2
    print(c)
    return 0


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest='cmd', required=True)

    p = sub.add_parser('list'); p.add_argument('--status'); p.set_defaults(f=cmd_list)
    p = sub.add_parser('show'); p.add_argument('name'); p.set_defaults(f=cmd_show)
    p = sub.add_parser('doctor'); p.add_argument('names', nargs='*'); p.set_defaults(f=cmd_doctor)
    p = sub.add_parser('plan'); p.add_argument('phase', choices=PHASES); p.add_argument('name')
    p.set_defaults(f=cmd_plan)
    p = sub.add_parser('check-provides'); p.add_argument('name')
    p.set_defaults(f=lambda a: check_provides(load_one(a.name)))
    p = sub.add_parser('check-jdk'); p.add_argument('name')
    p.set_defaults(f=lambda a: check_jdk(load_one(a.name)))
    p = sub.add_parser('baseline'); p.add_argument('name'); p.add_argument('--record', action='store_true')
    p.set_defaults(f=lambda a: baseline_cmd(load_one(a.name), a.record))

    a = ap.parse_args()
    sys.exit(a.f(a) or 0)


if __name__ == '__main__':
    main()
