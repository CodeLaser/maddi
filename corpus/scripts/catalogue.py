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
    $CORPUS_CATALOGUE is a colon-separated list, in precedence order, so a private overlay can add
    entries without this repo knowing it exists. Default: the catalogue/ next to this script's parent.

    A later file relates to an earlier entry of the same name in one of two EXPLICIT ways; two files
    with one name and neither key is an error, because an accidental collision would otherwise hide
    an entry without a word:
      extends: <name>   holds only the fields it adds or overrides. Merged field by field: mappings
                        recursively, lists and scalars replaced whole. `show` names each field's file.
      replaces: true    the whole entry, as before.

    ⛔ WHAT WE WRITE GOES BESIDE THE FILE THAT DECLARED THE FIELD, never beside the entry's first
    file. A private overlay may extend a public entry; a baseline or pin derived from the overlay's
    fields must land in the overlay's directory, not in this public repo. See `origin_of`.

MACHINE PROFILES
    $CORPUS_MACHINES is a colon-separated list of directories holding <hostname -s>.yml, one per
    machine: what it must hold, where its JDKs are. See machine_profile(). This repo ships none.

PINNING
    `source.rev` is the commit the corpus is measured at. Baselines are checked in and shared by every
    machine, so a checkout at another commit makes the diff about upstream rather than about maddi --
    and two machines at different commits re-record over each other. `build` and `baseline` refuse an
    off-pin checkout; `doctor` reports it. An entry without `source` that shares a checkout
    (fernflower-plugin) is pinned by the entry that owns that checkout.

    catalogue.py list   [--status active]     one line per entry
    catalogue.py show   <name>                the resolved entry, and which file each field came from
    catalogue.py doctor [<name>...]           obtained? at its pin? built? configured? per corpus --
                                              and, with a machine profile, is what it holds complete
    catalogue.py machine [--init]             check this host's profile; --init drafts one
    catalogue.py plan   <phase> <name>        print the shell command a phase would run
    catalogue.py obtain <name>                clone if absent, check out source.rev
    catalogue.py pin    <name> [--rev REV]    write the checkout's HEAD (or REV) as source.rev
    catalogue.py vendor <name> [--dry-run]    move the jars its configuration names into lib/<project>
    catalogue.py check-rev <name>             assert the checkout is at source.rev (exit 1 if not)
    catalogue.py check-provides <name>        assert build.provides exists  (exit 1 if not)
    catalogue.py check-jdk <name>             assert build.jdk is satisfied (exit 1 if not)
    catalogue.py baseline <name> [--record]   source-set inventory: diff against the recorded one
    catalogue.py dir    <name>                where its phases run (absolute; private entries
                                              live outside the corpus root)
    catalogue.py generates [<name>...]        every artefact we write into the corpus checkouts

`plan` prints rather than executes: Task runs the command so that its output streams and its exit
code is Task's, and so that `--dry` shows something real.
"""
import argparse
import copy
import datetime
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
PHASES = ('build', 'config', 'parse', 'analyse', 'tests')

# What each config route writes into the checkout, when the entry does not say. An entry's own
# `config.generates` wins; this exists so a new entry need not repeat the obvious, and so a
# declared list that OMITS a route's output is reported rather than silently trusted.
ROUTE_GENERATES = {
    'maven-plugin': ['inputConfiguration.json'],
    'gradle-plugin': ['inputConfiguration.json'],
    'maven-log': ['compile.log', 'compile.javac.log', 'inputConfiguration.json'],
    'gradle-log': ['compile.log', 'inputConfiguration.json'],
    'gradle-log-kotlin': ['compile.log', 'inputConfiguration.json'],
    'script': ['inputConfiguration.json'],
}


# ---------------------------------------------------------------- loading

def _path(s):
    """Expand ~ AND $VARS. Task interpolates its own vars but leaves shell ones alone, so any path
    reaching us from a Taskfile may still contain a literal $HOME."""
    return Path(os.path.expandvars(os.path.expanduser(str(s))))


def catalogue_dirs():
    """The catalogue directories, in precedence order (later wins on a name collision).

    expandvars AS WELL AS expanduser: Task interpolates its vars but leaves shell ones alone, so
    an overlay setting CORPUS_CATALOGUE from a GIT_ROOT of "$HOME/git" hands us a literal $HOME.
    With only expanduser that directory silently does not resolve, load_all() skips it, and the
    PUBLIC catalogue disappears from every listing while the private one still works -- which looks
    like a catalogue with one entry rather than like a broken path.
    """
    raw = os.environ.get('CORPUS_CATALOGUE') or str(DEFAULT_CATALOGUE)
    out = []
    for d in raw.split(':'):
        if not d:
            continue
        expanded = _path(d)
        if '$' in str(expanded):
            sys.exit(f'CORPUS_CATALOGUE entry {d!r} still contains an unexpanded variable')
        out.append(expanded)
    return out


def _record_origin(origin, value, file, path):
    origin[path] = file
    if isinstance(value, dict):
        for k, v in value.items():
            _record_origin(origin, v, file, path + (k,))


def _merge(base, over, origin, file, prefix=()):
    """Merge `over` into `base` in place, noting in `origin` which file each field now comes from.
    Mappings merge recursively; anything else replaces whole, and so forgets the origins below it."""
    for k, v in over.items():
        path = prefix + (k,)
        if isinstance(v, dict) and isinstance(base.get(k), dict):
            origin[path] = file
            _merge(base[k], v, origin, file, path)
            continue
        for p in [p for p in origin if p[:len(path)] == path]:
            del origin[p]
        base[k] = copy.deepcopy(v)
        _record_origin(origin, v, file, path)


def origin_of(entry, dotted):
    """The catalogue file that declared `dotted` (e.g. 'config.baseline'), or that declared its nearest
    enclosing mapping when the field itself is absent -- which is where a NEW value for it belongs."""
    path = tuple(dotted.split('.'))
    origin = entry.get('_origin') or {}
    while path:
        if path in origin:
            return Path(origin[path])
        path = path[:-1]
    return Path(entry['_file'])


def load_all():
    """-> {name: entry}. See CATALOGUE DIRECTORIES for how a later file relates to an earlier one."""
    out = {}
    for d in catalogue_dirs():
        if not d.is_dir():
            continue
        for f in sorted(d.glob('*.yml')) + sorted(d.glob('*.yaml')):
            e = parse_yaml(f.read_text(), str(f)) or {}
            base = e.pop('extends', None)
            replaces = e.pop('replaces', False)
            if base is not None:
                if base not in out:
                    sys.exit(f"{f}: extends '{base}', which no earlier catalogue file defines")
                if e.pop('name', base) != base:
                    sys.exit(f"{f}: `extends: {base}` and a different `name`; drop the name")
                _merge(out[base], e, out[base]['_origin'], str(f))
                out[base]['_files'].append(str(f))
                continue
            name = e.get('name') or f.stem
            if name in out and not replaces:
                sys.exit(f"{f}: '{name}' is already defined by {out[name]['_file']}. Say "
                         f"`extends: {name}` to add fields to it, or `replaces: true` to replace it")
            entry, origin = {}, {}
            _merge(entry, e, origin, str(f))
            entry['name'] = name
            entry.update(_file=str(f), _files=[str(f)], _origin=origin)
            out[name] = entry
    return out


def load_one(name):
    all_ = load_all()
    if name not in all_:
        sys.exit(f"no catalogue entry '{name}' in {':'.join(str(d) for d in catalogue_dirs())}")
    return all_[name]


def oss_root():
    return _path(os.environ.get('TEST_OSS_ROOT') or (Path.home() / 'git' / 'test-oss'))


# ---------------------------------------------------------------- machine profiles

def host_name():
    """CORPUS_HOST, else `hostname -s` -- the same name the gate records in last-green.json."""
    h = os.environ.get('CORPUS_HOST')
    if h:
        return h
    p = subprocess.run(['hostname', '-s'], capture_output=True, text=True)
    return p.stdout.strip() or os.uname().nodename.split('.')[0]


def machine_profile():
    """This host's profile, or None: <host>.yml in the first $CORPUS_MACHINES directory that has one.

    A profile says what THIS machine is supposed to hold and where its JDKs are. It is the third kind
    of fact next to the entry (what the project is) and `state()` (what is here): the machine's own
    declaration. Profiles name hosts and home-directory paths, so they belong with the private
    catalogue; this repo carries the reader and no profile.

        host: laser1                      # must match the file name
        os: linux                         # linux | macos -- informational, and checked
        role: gate                        # gate | devel -- informational
        test_oss_root: ~/git/test-oss     # checked against the effective TEST_OSS_ROOT
        jdks:                             # build.jdk.version picks one. A list, not a mapping
          - {version: 21, home: /usr/lib/jvm/java-21-openjdk-amd64}   # keyed by version: YAML reads
          - {version: 26, home: /usr/lib/jvm/java-26-openjdk-amd64}   # `21:` as an int, miniyaml not at all
        holds: active                     # every `status: active` entry, or a list of names
        skip:                             # exceptions to `holds`, each with its reason
          vavr: not checked out here yet
    """
    raw = os.environ.get('CORPUS_MACHINES') or ''
    host = host_name()
    for d in raw.split(':'):
        if not d:
            continue
        f = _path(d) / f'{host}.yml'
        if f.is_file():
            prof = parse_yaml(f.read_text(), str(f)) or {}
            if prof.get('host', host) != host:
                sys.exit(f"{f}: host is {prof.get('host')!r} but the file is this host's ({host})")
            prof['host'] = host
            prof['_file'] = str(f)
            jdks = prof.get('jdks') or []
            if not isinstance(jdks, list) or not all(isinstance(j, dict) and {'version', 'home'} <= set(j)
                                                     for j in jdks):
                sys.exit(f'{f}: jdks must be a list of {{version: N, home: PATH}}')
            prof['jdks'] = {str(j['version']): str(_path(j['home'])) for j in jdks}
            return prof
    return None


def expected_here(entry, profile):
    """-> (expected, reason). Without a profile every entry is expected, as before profiles existed."""
    if profile is None:
        return True, None
    skip = profile.get('skip') or {}
    if entry['name'] in skip:
        return False, skip[entry['name']]
    holds = profile.get('holds', 'active')
    if holds == 'active':
        return entry.get('status') == 'active', None
    return entry['name'] in (holds or []), None


def build_java_home(entry, profile=None):
    """The JDK this entry's BUILD runs on, or None for the ambient one. BUILD_JAVA_HOME wins; else the
    profile's JDK for `build.jdk.version`. maddi itself always runs on the ambient JDK."""
    if os.environ.get('BUILD_JAVA_HOME'):
        return os.environ['BUILD_JAVA_HOME']
    want = ((entry.get('build') or {}).get('jdk') or {}).get('version')
    if want is None:
        return None
    return ((profile if profile is not None else machine_profile() or {}).get('jdks') or {}).get(str(want))


def _java_props(home):
    try:
        p = subprocess.run([str(Path(home) / 'bin' / 'java'), '-XshowSettings:properties', '-version'],
                           capture_output=True, text=True, timeout=30)
    except Exception:
        return None
    return p.stderr + p.stdout


def _java_version(home):
    m = re.search(r'java\.specification\.version = (\d+)', _java_props(home) or '')
    return m.group(1) if m else None


def discover_jdks():
    """-> {major: home} over the usual install locations, first found per version. For `machine --init`."""
    globs = ['/usr/lib/jvm/*', '/Library/Java/JavaVirtualMachines/*/Contents/Home',
             '~/Library/Java/JavaVirtualMachines/*/Contents/Home', '~/.sdkman/candidates/java/*',
             '~/.gradle/jdks/*']
    found = {}
    for g in globs:
        base = Path(os.path.expanduser(g))
        for home in sorted(Path(base.anchor).glob(str(base.relative_to(base.anchor)))):
            if home.is_symlink() or not (home / 'bin' / 'java').is_file():
                continue
            v = _java_version(home)
            if v and v not in found:
                found[v] = str(home)
    return dict(sorted(found.items(), key=lambda kv: int(kv[0])))


def project_dir(entry):
    """Where the sources are. Relative to TEST_OSS_ROOT for a corpus project; a private one sets an
    absolute `dir` (~ and $VARS expanded), because customer checkouts do not live in the corpus."""
    d = str(_path(entry.get('dir') or entry['name']))
    return Path(d) if os.path.isabs(d) else oss_root() / d


# ---------------------------------------------------------------- pinning

def checkout_owner(entry, all_=None):
    """The entry whose `source` this entry's checkout comes from: itself, or -- for an entry with no
    `source` that shares a `dir` (fernflower-plugin, pulsar-plugin) -- the one that clones that tree.
    None when nobody owns it (a copy-only tree). A shared tree has ONE pin, the owner's."""
    if entry.get('source'):
        return entry
    d = project_dir(entry)
    for other in (all_ if all_ is not None else load_all()).values():
        if other.get('source') and project_dir(other) == d:
            return other
    return None


def _git(d, *args):
    """-> stdout stripped, or None when git fails (not a repo, unknown rev, ...)."""
    p = subprocess.run(['git', '-C', str(d), *args], capture_output=True, text=True)
    return p.stdout.strip() if p.returncode == 0 else None


def rev_state(entry, all_=None):
    """-> {owner, pinned, head, at_pin, dirty}, all computed on THIS machine.

    `dirty` counts TRACKED modifications only: every corpus carries untracked files of ours
    (inputConfiguration.json, compile.log -- see `generates`), and those do not change what is parsed.
    `at_pin` is None when there is nothing to compare (unpinned, absent, or not a git checkout).
    """
    owner = checkout_owner(entry, all_)
    pinned = ((owner or {}).get('source') or {}).get('rev')
    d = project_dir(entry)
    head = _git(d, 'rev-parse', 'HEAD') if d.is_dir() else None
    at_pin = None
    if pinned and head:
        # rev-parse rather than string equality, so an abbreviated or tag-valued rev still compares;
        # `pin` itself always writes the full SHA.
        at_pin = _git(d, 'rev-parse', '--verify', '--quiet', f'{pinned}^{{commit}}') == head
    dirty = bool(head) and bool(_git(d, 'status', '--porcelain', '--untracked-files=no'))
    return {'owner': (owner or {}).get('name'), 'pinned': pinned, 'head': head,
            'at_pin': at_pin, 'dirty': dirty}


def check_rev(entry, all_=None, require_pin=False):
    """0 when the checkout is at its pin. An UNPINNED entry passes with a warning -- unless
    `require_pin`, which recording a baseline asks for: a number recorded at no known commit is one
    the next machine cannot reproduce."""
    r = rev_state(entry, all_)
    name = entry['name']
    if not r['pinned']:
        if r['owner'] is None:
            print(f'{name}: no source owns this checkout -- nothing to pin', file=sys.stderr)
            return 1 if require_pin else 0
        print(f"{name}: UNPINNED -- `catalogue.py pin {r['owner']}` records the current commit",
              file=sys.stderr)
        return 1 if require_pin else 0
    if r['head'] is None:
        print(f'{name}: {project_dir(entry)} is not a git checkout -- `catalogue.py obtain '
              f"{r['owner']}`", file=sys.stderr)
        return 1
    if not r['at_pin']:
        print(f"{name}: checkout is at {r['head'][:12]}, pinned at {r['pinned'][:12]} "
              f"(by {r['owner']}) -- `catalogue.py obtain {r['owner']}`, or move the pin with "
              f'`catalogue.py pin {r["owner"]}` and re-record the baseline', file=sys.stderr)
        return 1
    if r['dirty']:
        print(f'{name}: at its pin, but tracked files are modified -- `git -C {project_dir(entry)} '
              f'status`', file=sys.stderr)
    return 0


def obtain(entry):
    """Clone if absent, then check out `source.rev`. Runs git itself rather than printing a plan:
    there is no build tool output to stream and no exit code worth handing to Task."""
    name = entry['name']
    s = entry.get('source') or {}
    if s.get('kind') != 'git':
        print(f"{name}: source.kind is {s.get('kind')!r}, not git -- obtain it by hand "
              f'(copy-only: rsync from a machine that has it)', file=sys.stderr)
        return 1
    d = project_dir(entry)
    if not d.exists():
        # A FULL clone, whatever the rev: Maven version plugins read history (langchain4j).
        print(f">>> {name}: cloning {s['url']} into {d}", file=sys.stderr)
        if subprocess.run(['git', 'clone', s['url'], str(d)]).returncode != 0:
            return 1
    rev = s.get('rev')
    if not rev:
        print(f'{name}: UNPINNED -- left at whatever the clone checked out. Pin it with '
              f'`catalogue.py pin {name}` once a baseline is recorded against it', file=sys.stderr)
        return 0
    if _git(d, 'rev-parse', '--verify', '--quiet', f'{rev}^{{commit}}') is None:
        print(f'>>> {name}: fetching {rev}', file=sys.stderr)
        subprocess.run(['git', '-C', str(d), 'fetch', 'origin', rev])
        if _git(d, 'rev-parse', '--verify', '--quiet', f'{rev}^{{commit}}') is None:
            print(f'{name}: {rev} is not in {s["url"]}', file=sys.stderr)
            return 1
    if _git(d, 'status', '--porcelain', '--untracked-files=no'):
        # Never discard someone's edits to a checkout; a reset is theirs to decide.
        print(f'{name}: tracked files are modified in {d}; refusing to check out {rev[:12]} over '
              f'them', file=sys.stderr)
        return 1
    if subprocess.run(['git', '-C', str(d), 'checkout', '--quiet', '--detach', rev]).returncode:
        return 1
    print(f'{name}: at {rev[:12]}', file=sys.stderr)
    return 0


_SOURCE_BLOCK = re.compile(r'^source:[ \t]*(#.*)?$', re.M)


def pin(entry, rev=None):
    """Write the checkout's HEAD (or `rev`, resolved to a full SHA) as `source.rev`, into the file
    that declared `source` -- a private overlay's source is pinned in the overlay.

    A text edit, not a YAML round-trip: the entries are mostly comments, and they are the point.
    """
    name = entry['name']
    if not entry.get('source'):
        print(f'{name}: no `source` -- pin the entry that owns the checkout', file=sys.stderr)
        return 1
    d = project_dir(entry)
    sha = _git(d, 'rev-parse', '--verify', '--quiet', f"{rev or 'HEAD'}^{{commit}}")
    if not sha:
        print(f"{name}: cannot resolve {rev or 'HEAD'} in {d}", file=sys.stderr)
        return 1
    f = origin_of(entry, 'source.rev' if 'rev' in entry['source'] else 'source.url')
    lines = f.read_text().splitlines(keepends=True)
    m = _SOURCE_BLOCK.search(''.join(lines))
    if not m:
        print(f'{f}: no block-style `source:` to edit -- add `rev: {sha}` by hand', file=sys.stderr)
        return 1
    start = ''.join(lines).count('\n', 0, m.start()) + 1       # first line after `source:`
    end = start
    while end < len(lines) and (lines[end].startswith((' ', '\t')) or not lines[end].strip()):
        end += 1
    block = range(start, end)
    rev_line = next((i for i in block if re.match(r'\s+rev:', lines[i])), None)
    if rev_line is not None:
        indent = re.match(r'\s+', lines[rev_line]).group(0)
        lines[rev_line] = f'{indent}rev: {sha}\n'
    else:
        url_line = next((i for i in block if re.match(r'\s+url:', lines[i])), None)
        anchor = url_line if url_line is not None else start - 1
        indent = re.match(r'\s*', lines[url_line]).group(0) if url_line is not None else '  '
        lines.insert(anchor + 1, f'{indent}rev: {sha}\n')
    f.write_text(''.join(lines))
    # Trust nothing about the edit until the file says so.
    if ((parse_yaml(f.read_text(), str(f)) or {}).get('source') or {}).get('rev') != sha:
        sys.exit(f'{f}: wrote source.rev but it does not read back as {sha} -- check the file')
    print(f'{name}: pinned at {sha} in {f}', file=sys.stderr)
    return 0


# ---------------------------------------------------------------- state

def state(entry, all_=None):
    """What is true on THIS machine — always computed, never stored in the entry."""
    d = project_dir(entry)
    provides = (entry.get('build') or {}).get('provides') or []
    return {
        'present': d.is_dir(),
        'built': bool(provides) and all((d / p).exists() for p in provides),
        'configured': config_path(entry).is_file(),
        'buildable': bool((entry.get('build') or {}).get('cmd')),
        'obtainable': (entry.get('source') or {}).get('kind') in ('git',),
        'rev': rev_state(entry, all_),
    }


def config_path(entry):
    """Where this entry's inputConfiguration.json lives -- see config.output.

    ABSOLUTE `output` is the private-project case: the refactor server's work dir, which is not under
    TEST_OSS_ROOT at all.

    RELATIVE `output` is resolved against the project dir, and that is what makes an A/B PAIR
    expressible: two entries over ONE checkout, each writing its own configuration beside the sources.
    Without it the second entry would have to spell an absolute path containing $TEST_OSS_ROOT, which
    `_path` expands only when that variable happens to be set in the environment -- so the same entry
    would resolve to a different place depending on who invoked it, and `generates` (the preserve-list
    `git clean` is driven from) would name a file nothing writes.
    """
    c = entry.get('config') or {}
    if c.get('output'):
        out = _path(c['output'])
        return out if out.is_absolute() else project_dir(entry) / out
    return project_dir(entry) / 'inputConfiguration.json'


def parse_config_path(entry):
    """The configuration the parse and analyse phases read: `parse.config` when the entry names one
    (resolved like `config.output`), else the one the config phase writes.

    vavr is the case: its route writes inputConfiguration.json with a test source set that cannot
    parse, and a script derives inputConfiguration.main.json from it. The parse is of the derived one.
    """
    p = (entry.get('parse') or {}).get('config')
    if not p:
        return config_path(entry)
    out = _path(p)
    return out if out.is_absolute() else project_dir(entry) / out


def source_sets(entry, path=None):
    """-> [(name, is_test)] from the generated config (or `path`), or [] when there is none."""
    f = path or config_path(entry)
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


def generates(entry):
    """-> (paths relative to the project dir, [warnings]).

    Everything of OURS inside a third-party checkout. It matters because these files are untracked:
    `git reset --hard` leaves them alone, but `git clean -fdx` deletes them — and the corpus
    pre-flight needs a preserve-list that cannot rot. Deriving it from here is what stops that list
    being maintained by hand, which is how it would go stale the next time a route changes.
    """
    cfg = entry.get('config') or {}
    route = cfg.get('route')
    declared = cfg.get('generates')
    default = ROUTE_GENERATES.get(route, [])
    warn = []
    if declared is None:
        paths = list(default)
    else:
        paths = list(declared)
        missing = [p for p in default if p not in paths]
        if missing:
            warn.append(f"{entry['name']}: config.generates omits {missing}, which route "
                        f"{route!r} writes")
    # Artefacts of ours that no phase writes — fernflower's Eclipse-plugin-demo .project/.classpath.
    paths += list(entry.get('generates') or [])

    # Resolve to ABSOLUTE paths, because a preserve-list of relative ones is ambiguous the moment an
    # entry is not under TEST_OSS_ROOT. And inputConfiguration.json does not necessarily live beside
    # the sources: a private entry writes it to config.output, the refactor server's work dir.
    d = project_dir(entry)
    resolved = []
    for rel in paths:
        resolved.append(config_path(entry) if rel == 'inputConfiguration.json' else d / rel)
    return resolved, warn


# ---------------------------------------------------------------- phases

def plugin_version():
    """MADDI_PLUGIN_VERSION, else the `version=` of the maddi checkout's gradle.properties -- the same
    default the Taskfile computes, and what `config:plugin` publishes.

    ⛔ It used to be the environment variable or ''. `_cat` never exported it, so `catalogue:config` on
    a plugin route asked Gradle for `io.codelaser:maddi-gradleplugin:` -- no version -- and died in
    four seconds on "Could not find". Reading the file here removes the dependency on who invoked us.
    """
    v = os.environ.get('MADDI_PLUGIN_VERSION')
    if v:
        return v
    props = Path(os.environ.get('MADDI_REPO') or HERE.parent.parent) / 'gradle.properties'
    m = re.search(r'^version=(\S+)', props.read_text(), re.M) if props.is_file() else None
    if not m:
        sys.exit(f'no MADDI_PLUGIN_VERSION and no version= in {props}')
    return m.group(1)


def _mvn_exclusions(cfg):
    ex = cfg.get('exclude_modules') or []
    return f" -pl '{','.join(ex)}'" if ex else ''


def _runner_module(entry):
    return {'openjdk': 'maddi-run-openjdk', 'kotlin': 'maddi-run-kotlin',
            'main': 'maddi-run-main'}[(entry.get('parse') or {}).get('runner', 'openjdk')]


def _run_maddi(entry, steps):
    maddi = Path(os.environ.get('MADDI_REPO') or HERE.parent.parent).resolve()
    return (f'{maddi}/gradlew -q -p {maddi} :{_runner_module(entry)}:run '
            f'--args="--input-configuration={parse_config_path(entry)} --analysis-steps={steps}"')


# ⛔ The Kotlin driver's `--analysis-steps=none` logs "nothing to do" and returns BEFORE parsing
# (maddi-run-kotlin Main.runMixed), so a parse phase for a Kotlin entry would pass having read nothing,
# and a baseline would have nothing to count -- the Java side's per-source-set line comes from
# ScanCompilationUnits, which the mixed path does not print. Refused rather than run vacuously.
KOTLIN_NO_PARSE_ONLY = ("{name}: runner kotlin has no parse-only mode yet -- `--analysis-steps=none` stops "
                        "before parsing, so this phase would pass having read nothing. Use the analyse "
                        "phase (its parse.test), which parses and asserts its floors.")


# `rewrite_reactor_jars: true` on a maven-log route captured under `install` (guava, ignite-core): once a
# module is packaged, Maven hands its siblings the JAR, so the captured -classpath names
# <module>/target/<a>.jar and maddi would find those types twice -- as a parsed source set and in the jar.
# ⛔ DELETING the entries is wrong: it is the only link from a module to its sibling (guava-testlib stops
# seeing guava; TestGuava dies in 17 s). REWRITING to target/classes and target/test-classes is what
# `test-compile` would have emitted. -tests first, the more specific pattern. Anchored at the project dir so
# ~/.m2 jars are untouched; `realpath` because the root may hold ../.. while the log is normalised. Fails
# if any reactor jar survives, since a pattern that silently rewrote nothing looks exactly like success.
# The same shell as the Taskfile's _config:maven-log REWRITE_REACTOR_JARS, of which this is the port.
_REWRITE_REACTOR_JARS = (
    'd=$(realpath .) && '
    'before=$(grep -oE "$d/[^:]*/target/[^:]*\\.jar" compile.javac.log | wc -l) && '
    'sed -E "s#($d/[^:]*)/target/[^:]*-tests\\.jar#\\1/target/test-classes#g; '
    's#($d/[^:]*)/target/[^:]*\\.jar#\\1/target/classes#g" '
    'compile.javac.log > compile.javac.log.tmp && mv compile.javac.log.tmp compile.javac.log && '
    'after=$(grep -cE "$d/[^:]*/target/[^:]*\\.jar" compile.javac.log || true) && '
    'echo ">>> rewrote $before reactor-jar classpath entries to output dirs (remaining: $after)" && '
    'test "$after" -eq 0 && ')


def plan(entry, phase):
    """-> the shell command for one phase, or None when the entry does not define it."""
    name = entry['name']
    d = project_dir(entry)

    if phase == 'build':
        b = entry.get('build') or {}
        if not b.get('cmd'):
            return None
        jh = build_java_home(entry)
        return f'JAVA_HOME={jh} {b["cmd"]}' if jh else b['cmd']

    if phase == 'config':
        c = entry.get('config') or {}
        route = c.get('route')
        if not route or route == 'none':
            return None
        maddi = Path(os.environ.get('MADDI_REPO') or HERE.parent.parent).resolve()
        # A corpus project's config sits beside its sources, where TestOssCorpus.config() looks.
        # A private project's belongs in the refactor server's work dir, which is what
        # ProjectServiceImpl.load reads -- so `config.output` overrides.
        # ⛔ config_path(), NOT a second copy of its rule. This line WAS that second copy, and it
        # diverged the moment `output` learned to be relative: it produced `mkdir -p .` and handed the
        # build tool a relative -D property, so the file landed wherever the tool's cwd happened to be
        # while every other reader looked for it beside the sources. One rule, one function.
        out = config_path(entry)
        # Nothing else creates the output's directory. maddi's Main opens the file and dies with a
        # bare `FileNotFoundException: ... (No such file or directory)` -- after the full -X rebuild,
        # so the whole cost of the phase is paid before the failure. It never bit while every config
        # sat beside its sources (that directory necessarily exists); the first PRIVATE entry writes
        # into the refactor server's work/<name>/, which the SERVER creates when it registers a
        # project and nobody creates when the corpus driver gets there first.
        mk = f'mkdir -p {out.parent} && '
        # project.yml records extra_jmods per project (closed-core needs jdk.javadoc +
        # jdk.compiler); without them those modules are simply absent from the classpath.
        jmods = ''.join(f' --extra-jmod {j}' for j in (c.get('extra_jmods') or []))
        if route == 'maven-plugin':
            ver = plugin_version()
            # A project's build may force switches on us that this invocation has to repeat: it is a
            # separate `mvn` run from the build phase and inherits nothing from it. jenkins is the case
            # -- its maven.config activates a profile carrying an enforcer rule that the pinned enforcer
            # cannot load, so without -Denforcer.skip=true the run dies before the mojo is reached. The
            # Gradle counterpart of this field is `gradle_args`.
            flags = f' {c["mvn_flags"]}' if c.get('mvn_flags') else ''
            return mk + (f'MAVEN_OPTS="$MADDI_EXPORTS -Xmx{c.get("mem", "6G")}" '
                    f'mvn{flags} -pl {c["module"]} generate-test-sources '
                    f'io.codelaser:maddi-mvnplugin:{ver}:write-input-configuration'
                    f' && cp {c["module"]}/target/inputConfiguration.json {out}')
        if route == 'gradle-plugin':
            # The Gradle counterpart of `maven-plugin`, and the ONLY route that exercises
            # maddi-gradleplugin against a real project -- everything else about that plugin is
            # tested by `dogfood`, which is maddi's own code.
            #
            # ⚠ WHAT THIS ROUTE IS FOR. Run it on a project that also has a `gradle-log` entry and
            # diff the two configurations: that is the A/B which found the plugin's source-set `uri`
            # defect (2026-08-19, fernflower -- one dropped compilation unit that --compile-log did
            # not lose). The plugins are invoked per module and see siblings as jars, while the log
            # route sees a whole reactor at once, so the two are expected to differ in SHAPE; what
            # the A/B checks is that they agree on what PARSES.
            #
            # The plugin is applied by an init script rather than by editing the checkout -- see
            # scripts/maddi-plugin.init.gradle.kts for why that matters to `generates`.
            ver = plugin_version()
            init = HERE / 'maddi-plugin.init.gradle.kts'
            # A Gradle PROJECT PATH (':libs:core'), not a directory: absent or ':' is the root
            # project, which is what a single-project build like fernflower has.
            module = (c.get('module') or '').strip().strip(':')
            prefix = f':{module}' if module else ''
            # Every other route states extra_jmods as ADDITIONS to the java.se closure
            # (`--extra-jmod` adds to it). The plugin takes one list instead, so the default has to
            # be named alongside them or asking for an extra would silently drop the rest.
            extra = c.get('extra_jmods') or []
            jmods_prop = f' -Dmaddi.jmods=java.se,{",".join(extra)}' if extra else ''
            # ⚠ A CORPUS'S BUILD MAY REFUSE TO CONFIGURE WITHOUT ITS OWN FLAGS, and this route builds
            # the task name itself, so there is nowhere for them to ride along -- `gradle-log` smuggles
            # them through `tasks`, which is a string it interpolates whole. pulsar is the case:
            # `-PskipJavaVersionCheck` or its settings script rejects JDK 26 before any task exists.
            args = f' {c["gradle_args"]}' if c.get('gradle_args') else ''
            # Which projects the init script applies the plugin to -- `all` (its default, the dogfood
            # pattern: siblings publish SOURCES and are co-parsed) or one project path (siblings arrive
            # as ordinary class-path artifacts). Two different tests; see the init script's comment.
            apply_to = f' -Dmaddi.applyTo={c["apply_to"]}' if c.get('apply_to') else ''
            # --refresh-dependencies: the plugin's version does not change from one publication to
            # the next, so Gradle otherwise serves the cached jar and this silently runs the
            # PREVIOUS plugin (the same trap dogfood's GradleBuild task documents).
            return mk + (f'./gradlew --no-build-cache --refresh-dependencies '
                    f'--init-script {init}{args} '
                    f'-Dmaddi.pluginVersion={ver}{jmods_prop}{apply_to} -Dmaddi.outputFile={out} '
                    f'{prefix}:maddi-write-input-configuration')
        if route == 'maven-log':
            jh = f'JAVA_HOME={c["build_java_home"]} ' if c.get('build_java_home') else ''
            # `clean` is mandatory: maven-compiler-plugin skips an up-to-date module and a skipped
            # module emits no "Command line options:" line at all, so capturing over an already
            # built reactor yields a SILENTLY PARTIAL config -- measured on timefold, 22 source
            # sets instead of 65, missing core/main. Nothing downstream reveals the loss.
            # `cmd` verbatim when the project supplies one -- private projects carry their own in
            # project.yml, and it is not always `clean`: callforpapers uses
            # `-Dmaven.build.cache.enabled=false` to force every module to recompile, which is the
            # same guarantee by a different means. Composing a command over that would break it.
            build = c.get('cmd') or f'./mvnw -X clean {c["tasks"]}{_mvn_exclusions(c)}'
            # Extra maddi options for this configuration -- ignite-core's `--jre <JDK 17>`, because
            # nothing in a javac line says which JDK compiled it (see the Taskfile's config:ignite-core).
            margs = f' {c["maddi_args"]}' if c.get('maddi_args') else ''
            return mk + (f'{jh}MAVEN_OPTS="$MADDI_EXPORTS -Xmx{c.get("mem", "6G")}" '
                    f'{build} > compile.log 2>&1; '
                    # Filter BEFORE maddi reads it: ParseJavacList does readString on the whole
                    # file, and a >2GB log dies on the JVM's max array size, which no -Xmx fixes.
                    # Equivalent input, not a shortcut -- these are exactly the lines it keeps.
                    f"grep -aE '^\\[DEBUG] -d ' compile.log > compile.javac.log && "
                    + (_REWRITE_REACTOR_JARS if c.get('rewrite_reactor_jars') else '')
                    + f'{maddi}/gradlew -p {maddi} :maddi-run-openjdk:run '
                    f'--args="--compile-log {d}/compile.javac.log{jmods}{margs} '
                    f'--write-input-configuration {out}"')
        if route in ('gradle-log', 'gradle-log-kotlin'):
            target = 'maddi-run-kotlin' if route.endswith('kotlin') else 'maddi-run-openjdk'
            # The kotlinc marker is ParseKotlincList.GRADLE_PATTERN's -- test_catalogue.py holds the
            # two together. This line once read `[KOTLIN] compiler arguments:`, which occurs 0 times in
            # detekt's real --debug log against 32 of the right one: a javac-only config, silently.
            grep = ("grep -aE 'Compiler arguments:|Kotlin compiler args:'"
                    if route.endswith('kotlin') else "grep -a 'Compiler arguments:'")
            extra = ' --no-configuration-cache -Dorg.gradle.warning.mode=summary' if route.endswith('kotlin') else ''
            return mk + (f'./gradlew --no-build-cache --rerun-tasks {c["tasks"]}{extra} --debug 2>&1 | '
                    f'{grep} > compile.log; '
                    f'{maddi}/gradlew -p {maddi} :{target}:run '
                    f'--args="--compile-log {d}/compile.log{jmods} '
                    f'--write-input-configuration {out}"')
        if route == 'script':
            return f'python3 {HERE / Path(c["script"]).name}'
        sys.exit(f'{name}: unknown config.route {route!r}')

    if phase == 'parse':
        if (entry.get('parse') or {}).get('runner') == 'kotlin':
            sys.exit(KOTLIN_NO_PARSE_ONLY.format(name=name))
        # PARSE ONLY -- `--analysis-steps=none`. This phase answers "does the config load, and
        # does everything in it parse", which is a property of the CONFIG. Running the analyzer
        # here instead conflates that with "is the analyzer working", so a red says nothing
        # about which of the two broke -- and it costs 60x more: fernflower is 13s parse-only
        # against 13m under `modification`, timefold 25m. The analyzer run is `analyse`, below.
        return _run_maddi(entry, 'none')

    if phase == 'analyse':
        # The ANALYZER's regression check over this corpus. Expensive, and separate on purpose.
        # With a `parse.test`, that maddi corpus test, at whatever --analysis-steps it declares. Without
        # one, `parse.steps` run directly -- the field every entry declares and, until 2026-09-26,
        # nothing read: vavr, camel and the -plugin entries had no analyse phase at all.
        p = entry.get('parse') or {}
        if p.get('test'):
            maddi = Path(os.environ.get('MADDI_REPO') or HERE.parent.parent).resolve()
            return (f'{maddi}/gradlew -p {maddi} :{_runner_module(entry)}:slowTest '
                    f"--tests '*{p['test']}' --rerun-tasks")
        if p.get('steps'):
            return _run_maddi(entry, ','.join(p['steps']))
        return None

    if phase == 'tests':
        return (entry.get('tests') or {}).get('cmd')

    sys.exit(f'unknown phase {phase!r}')


# --------------------------------------------------- parse-only measurement

# What a `--analysis-steps=none` run prints per source set. This is the ONLY place the primary-type
# count per source set is available: the analysis path logs a single total ("Running prep analyzer
# on {} types"), which is why an earlier version of this script recorded the source-set inventory
# instead and claimed the real thing needed a change on the maddi side. It did not.
# (.+?)$ with re.M, NOT (\S+): Maven source-set names contain SPACES -- 'LangChain4j :: 
# Core/main', 'Guava: Google Core Libraries for Java/test'. With \S+ every such name
# truncated at the first space and a project's source sets collapsed into ONE key, which
# read as a config with one source set rather than as a broken regex.
_COLLECTED = re.compile(r'Collected (\d+) class symbols for source set (.+?)\s*$', re.M)


def measure_parse(entry):
    """Run the parse-only phase and return {source set: primary types}.

    Raises RuntimeError with the tail of the output when the run fails -- a parse that does not
    complete has no counts, and reporting zero for every source set would look exactly like a
    corpus that vanished.
    """
    cmd = plan(entry, 'parse')
    proc = subprocess.run(cmd, shell=True, cwd=project_dir(entry),
                          capture_output=True, text=True)
    blob = proc.stdout + proc.stderr
    reported = {m.group(2): int(m.group(1)) for m in _COLLECTED.finditer(blob)}
    if proc.returncode != 0 or not reported:
        tail = '\n'.join(blob.splitlines()[-15:])
        raise RuntimeError(f"{entry['name']}: parse-only run failed "
                           f'(rc={proc.returncode}, {len(reported)} source sets seen)\n{tail}')

    # Seed EVERY source set the config declares at 0, then overlay what the parse reported. A
    # source set with no class symbols never logs a line -- timefold's spring-boot-starter/main
    # holds a single module-info.java and nothing else -- so without the seed its row would simply
    # be absent, and "this source set is empty" would be indistinguishable from "this source set
    # is gone". One is normal; the other is a config that lost a module.
    counts = {name: 0 for name, _ in source_sets(entry, parse_config_path(entry))}
    unknown = sorted(set(reported) - set(counts))
    if unknown:
        # The parse saw a source set the config does not declare: either the regex drifted or the
        # run read a different config. Either way the table would be wrong, so refuse it.
        raise RuntimeError(f"{entry['name']}: parse reported source sets absent from the config: "
                           f'{unknown}')
    counts.update(reported)
    return counts


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
    # The home the build phase will actually use (see build_java_home), else the ambient one.
    home = build_java_home(entry) or os.environ.get('JAVA_HOME')
    if not home:
        print(f"{entry['name']}: needs JDK {req} but neither BUILD_JAVA_HOME, this machine's profile "
              f'(jdks: {req.get("version")}) nor JAVA_HOME names one', file=sys.stderr)
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
    """Resolved beside the file that DECLARED `config.baseline`, not beside the entry's first file:
    a private overlay's baseline lives in the private catalogue."""
    b = (entry.get('config') or {}).get('baseline')
    if not b:
        return None
    return (origin_of(entry, 'config.baseline').parent / b).resolve()


def record_refusal(entry):
    """Why recording this entry's baseline would put private data in the wrong place, or None.

    ⛔ The measured numbers depend on every field the entry has, and an overlay that `extends` it may
    have changed any of them (config.output, parse.config, the route). A baseline declared in an
    EARLIER catalogue directory than the entry's last file would then carry numbers derived from a
    private overlay into a public repo. So the baseline must be declared by the last file to touch
    the entry. Comparing is read-only and stays allowed.
    """
    last = Path(entry['_files'][-1]).parent.resolve()
    declared = origin_of(entry, 'config.baseline').parent.resolve()
    if declared == last:
        return None
    return (f"{entry['name']}: config.baseline is declared in {declared}, but {entry['_files'][-1]} "
            f'extends the entry, so what it measures may depend on that overlay. Declare '
            f'config.baseline in the overlay and record there')


def baseline_cmd(entry, record, all_=None, if_declared=False):
    """Primary types PER SOURCE SET, from a parse-only run: diff against the recorded table.

    Per source set rather than one total, because a total hides coverage moving BETWEEN modules,
    which is the drift worth catching. Recorded rather than hand-written, because timefold has 65
    source sets and pulsar 90 -- a table nobody maintains is worse than no table.

    It also catches the failure that has actually bitten: capturing timefold's compile log without
    `clean` yields 22 source sets instead of 65, missing core/main, and the result loads and
    analyses without complaint. Invisible to every other instrument; a missing row here.
    """
    p = baseline_path(entry)
    if not p:
        # `catalogue:config` asks with --if-declared: an entry without a baseline has nothing to diff,
        # and that is not a failed configuration.
        print(f"{entry['name']}: no config.baseline declared", file=sys.stderr)
        return 0 if if_declared else 1
    if (entry.get('parse') or {}).get('runner') == 'kotlin':
        print(KOTLIN_NO_PARSE_ONLY.format(name=entry['name']), file=sys.stderr)
        return 1
    if record and (why := record_refusal(entry)):
        print(why, file=sys.stderr)
        return 1
    # Off the pin, the diff describes upstream, not maddi -- and a record there is one no other
    # machine can reproduce, so recording also insists that there IS a pin.
    if check_rev(entry, all_, require_pin=record):
        return 1
    if not parse_config_path(entry).is_file():
        print(f"{entry['name']}: no {parse_config_path(entry)} -- run the config phase first",
              file=sys.stderr)
        return 1
    try:
        got = measure_parse(entry)
    except RuntimeError as e:
        print(str(e), file=sys.stderr)
        return 1

    if record:
        p.parent.mkdir(parents=True, exist_ok=True)
        # WHERE it was measured, so a disagreement between machines has an explanation to start from.
        r = rev_state(entry, all_)
        maddi = _git(Path(os.environ.get('MADDI_REPO') or HERE.parent.parent), 'rev-parse', 'HEAD')
        p.write_text('# source set\tprimary types -- `catalogue.py baseline <name> --record`\n'
                     f"# recorded {datetime.date.today()} on {host_name()}, corpus "
                     f"{(r['head'] or '?')[:12]}, maddi {(maddi or '?')[:12]}\n"
                     + ''.join(f'{k}\t{v}\n' for k, v in sorted(got.items())))
        total = sum(got.values())
        print(f'recorded {len(got)} source sets, {total} primary types -> {p}', file=sys.stderr)
        return 0

    if not p.is_file():
        print(f'{p} does not exist -- record it with RECORD=1', file=sys.stderr)
        return 1
    want = {}
    for line in p.read_text().splitlines():
        if line.strip() and not line.startswith('#'):
            k, _, v = line.partition('\t')
            want[k] = int(v)
    added = sorted(set(got) - set(want))
    removed = sorted(set(want) - set(got))
    changed = sorted(k for k in set(want) & set(got) if want[k] != got[k])
    for k in removed:
        print(f'- {k}\t{want[k]}', file=sys.stderr)
    for k in added:
        print(f'+ {k}\t{got[k]}', file=sys.stderr)
    for k in changed:
        print(f'~ {k}\t{want[k]} -> {got[k]}', file=sys.stderr)
    if added or removed or changed:
        recorded = next((l for l in p.read_text().splitlines() if l.startswith('# recorded ')), None)
        if recorded:
            print(recorded, file=sys.stderr)
        print(f"{entry['name']}: parse drifted ({len(want)} source sets/{sum(want.values())} types "
              f'-> {len(got)}/{sum(got.values())}). Read the diff, then accept it with RECORD=1.',
              file=sys.stderr)
        return 1
    print(f"{entry['name']}: {len(got)} source sets, {sum(got.values())} primary types, unchanged",
          file=sys.stderr)
    return 0


# ---------------------------------------------------------------- commands

def _pin_word(r):
    """A short verdict on rev_state(): ok, OFF (at another commit), unpinned, or n/a."""
    if r['at_pin'] is None:
        return 'unpinned' if r['owner'] and not r['pinned'] else 'n/a' if not r['pinned'] else 'absent'
    return ('ok' if r['at_pin'] else 'OFF') + ('+dirty' if r['dirty'] else '')


def cmd_list(args):
    all_ = load_all()
    for name, e in sorted(all_.items()):
        if args.status and e.get('status') != args.status:
            continue
        s = state(e, all_)
        flags = ''.join(c if v else '-' for c, v in
                        (('P', s['present']), ('R', s['rev']['at_pin']), ('B', s['built']),
                         ('C', s['configured'])))
        print(f"{name:22} {e.get('status', '?'):10} {flags}  {e.get('summary', '')[:60]}")


def cmd_show(args):
    e = load_one(args.name)
    print(dump_yaml({k: v for k, v in e.items() if not k.startswith('_')}))
    if len(e['_files']) > 1:
        # Which file each top-level field (or, where it was merged, each leaf) came from -- the
        # question an overlay makes worth asking.
        print('# fields by origin:')
        leaves = sorted(p for p in e['_origin']
                        if not any(q != p and q[:len(p)] == p for q in e['_origin']))
        for p in leaves:
            print(f"#   {'.'.join(p):32} {e['_origin'][p]}")
    sets = source_sets(e)
    if sets:
        print(f'# source sets on disk: {len(sets)} '
              f'({sum(1 for _, t in sets if not t)} main, {sum(1 for _, t in sets if t)} test)')


def cmd_doctor(args):
    all_ = load_all()
    names = args.names or sorted(all_)
    profile = machine_profile()
    if profile:
        print(f"# {profile['host']} ({profile.get('role', '?')}, {profile['_file']}): "
              f'an entry this machine holds must be present, at its pin, built and configured')
    else:
        print(f'# no machine profile for {host_name()} in $CORPUS_MACHINES: nothing is required here')
    print(f"{'corpus':22} {'status':10} {'present':>8} {'pin':>9} {'built':>7} {'config':>7}  notes")
    print('-' * 96)
    rc = 0
    for n in names:
        e = all_[n] if n in all_ else None
        if e is None:
            print(f'{n:22} NOT IN CATALOGUE')
            rc = 1
            continue
        s = state(e, all_)
        notes = []
        expected, why_not = expected_here(e, profile)
        if profile and not expected:
            print(f"{n:22} {e.get('status', '?'):10} {'':>8} {'':>9} {'':>7} {'':>7}  "
                  f"not held here{': ' + why_not if why_not else ''}")
            continue
        if profile:
            # What the profile says this machine holds, it must actually hold. Without a profile the
            # same gaps are notes, as they always were: nothing says they should be filled here.
            gap = (not s['present'] or (s['buildable'] and not s['built'])
                   or (not s['configured'] and (e.get('config') or {}).get('route') not in (None, 'none')))
            if gap:
                notes.append('!! HELD HERE')
                rc = 1
        r = s['rev']
        if r['at_pin'] is False:
            notes.append(f"!! at {r['head'][:12]}, pinned {r['pinned'][:12]}")
            rc = 1
        if r['dirty']:
            notes.append('!! tracked files modified')
        if not s['present']:
            notes.append('clone it' if s['obtainable'] else 'COPY-ONLY: rsync from a machine that has it')
        elif s['buildable'] and not s['built']:
            notes.append('build incomplete: build.provides missing')
        elif not s['configured'] and (e.get('config') or {}).get('route') not in (None, 'none'):
            notes.append(f'no {config_path(e).name}: `catalogue:config NAME={n}`')
        if s['present'] and s['configured']:
            sets = source_sets(e)
            t = sum(1 for _, is_t in sets if is_t)
            notes.append(f'{len(sets)} source sets, {t} test')
            if not t and (e.get('tests') or {}).get('cmd'):
                notes.append('!! tests.cmd declared but config has NO test source sets')
                rc = 1
        print(f"{n:22} {e.get('status', '?'):10} {str(s['present']):>8} {_pin_word(r):>9} {str(s['built']):>7} "
              f"{str(s['configured']):>7}  {'; '.join(notes)}")
    return rc


def cmd_machine(args):
    """Show and check this host's profile; --init drafts one from what is installed here."""
    if args.init:
        jdks = discover_jdks()
        osname = {'Darwin': 'macos', 'Linux': 'linux'}.get(os.uname().sysname, os.uname().sysname)
        print(f'# Draft profile for {host_name()} -- review, then save as <private catalogue>/../machines/'
              f'{host_name()}.yml')
        print(f'host: {host_name()}\nos: {osname}\nrole: devel\ntest_oss_root: {oss_root()}')
        print('jdks:' + ('' if jdks else ' []'))
        for v, home in jdks.items():
            print(f'  - {{version: {v}, home: {home}}}')
        print('holds: active\nskip: {}')
        return 0
    prof = machine_profile()
    if not prof:
        print(f'no profile for {host_name()} in $CORPUS_MACHINES '
              f"({os.environ.get('CORPUS_MACHINES') or 'unset'}); `machine --init` drafts one",
              file=sys.stderr)
        return 1
    rc = 0
    print(f"{prof['host']}: {prof.get('role', '?')} on {prof.get('os', '?')} -- {prof['_file']}")
    want_os = {'Darwin': 'macos', 'Linux': 'linux'}.get(os.uname().sysname)
    if prof.get('os') and prof['os'] != want_os:
        print(f"  !! profile says os {prof['os']}, this is {want_os}")
        rc = 1
    if prof.get('test_oss_root'):
        declared, effective = _path(prof['test_oss_root']).resolve(), oss_root().resolve()
        ok = declared == effective
        print(f"  test_oss_root {declared}{'' if ok else f'  !! but TEST_OSS_ROOT resolves to {effective}'}")
        rc |= not ok
    for v, home in prof['jdks'].items():
        got = _java_version(home)
        ok = got == v
        bad = f"  !! reports {got or 'nothing (no bin/java?)'}"
        print(f"  jdk {v:>3}  {home}{'' if ok else bad}")
        rc |= not ok
    all_ = load_all()
    for n in [*(prof.get('skip') or {}), *([] if prof.get('holds', 'active') == 'active' else prof['holds'])]:
        if n not in all_:
            print(f'  !! names {n!r}, which no catalogue entry defines')
            rc = 1
    held = [n for n, e in sorted(all_.items()) if expected_here(e, prof)[0]]
    print(f'  holds {len(held)} of {len(all_)} entries: {" ".join(held)}')
    return int(bool(rc))


def vendor(entry, dry_run=False):
    """Copy the jars this entry's configuration(s) name out of the build tools' caches, into
    TEST_OSS_ROOT/lib/<project>/ -- what every Taskfile `_config:*` does after writing a config, and
    what `catalogue:config` did NOT do until 2026-09-26: its configurations kept naming ~/.gradle and
    ~/.m2, which Gradle's 30-day cleanup and a hand-cleared ~/.m2 take away. See vendor-libraries.py.

    A configuration outside the corpus root (a private entry writing to the refactor server's work
    dir) is not vendored: lib/ is the corpus's, and that file belongs to another lifecycle.
    """
    root = oss_root().resolve()
    cfgs = []
    for c in (config_path(entry), parse_config_path(entry)):
        if c not in cfgs and c.is_file():
            cfgs.append(c)
    if not cfgs:
        print(f"{entry['name']}: no configuration on disk to vendor", file=sys.stderr)
        return 1
    rc = 0
    for c in cfgs:
        if root not in c.resolve().parents:
            print(f"{entry['name']}: {c} is outside {root}; not vendored", file=sys.stderr)
            continue
        cmd = [sys.executable, str(HERE / 'vendor-libraries.py'), '--corpus', str(root), str(c)]
        rc |= subprocess.run(cmd + (['--dry-run'] if dry_run else [])).returncode
    return rc


def cmd_dir(args):
    """Where a phase has to run, absolute.

    Exists for the Taskfile. Task's `dir:` is a static template, so it can only compose
    TEST_OSS_ROOT/<name> and cannot ask the catalogue — wrong for a private entry, whose
    distinguishing mark is precisely an absolute `dir` outside the corpus root. Worse, Task
    CREATES a missing `dir:` instead of failing, so the wrong answer was silent: an empty new
    directory in the corpus root, and the build running inside it.
    """
    print(project_dir(load_one(args.name)))
    return 0


def cmd_generates(args):
    """The preserve-list, as paths under TEST_OSS_ROOT — feed it to whatever must not delete them."""
    all_ = load_all()
    rc = 0
    for n in (args.names or sorted(all_)):
        e = all_.get(n)
        if e is None:
            print(f'{n}: not in the catalogue', file=sys.stderr)
            rc = 1
            continue
        paths, warn = generates(e)
        for w in warn:
            print('WARNING ' + w, file=sys.stderr)
            rc = 1
        for p in paths:
            print(f"{p}{'' if p.exists() else '   # not present'}")
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
    p = sub.add_parser('machine'); p.add_argument('--init', action='store_true')
    p.set_defaults(f=cmd_machine)
    p = sub.add_parser('vendor'); p.add_argument('name'); p.add_argument('--dry-run', action='store_true')
    p.set_defaults(f=lambda a: vendor(load_one(a.name), a.dry_run))
    p = sub.add_parser('obtain'); p.add_argument('name')
    p.set_defaults(f=lambda a: obtain(load_one(a.name)))
    p = sub.add_parser('pin'); p.add_argument('name'); p.add_argument('--rev')
    p.set_defaults(f=lambda a: pin(load_one(a.name), a.rev))
    p = sub.add_parser('check-rev'); p.add_argument('name')
    p.set_defaults(f=lambda a: check_rev(load_one(a.name)))
    p = sub.add_parser('check-provides'); p.add_argument('name')
    p.set_defaults(f=lambda a: check_provides(load_one(a.name)))
    p = sub.add_parser('check-jdk'); p.add_argument('name')
    p.set_defaults(f=lambda a: check_jdk(load_one(a.name)))
    p = sub.add_parser('dir'); p.add_argument('name'); p.set_defaults(f=cmd_dir)
    p = sub.add_parser('generates'); p.add_argument('names', nargs='*')
    p.set_defaults(f=cmd_generates)
    p = sub.add_parser('baseline'); p.add_argument('name'); p.add_argument('--record', action='store_true')
    p.add_argument('--if-declared', action='store_true')
    p.set_defaults(f=lambda a: baseline_cmd(load_one(a.name), a.record, if_declared=a.if_declared))

    a = ap.parse_args()
    sys.exit(a.f(a) or 0)


if __name__ == '__main__':
    main()
