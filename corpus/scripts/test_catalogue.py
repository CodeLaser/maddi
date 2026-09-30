#!/usr/bin/env python3
"""
Tests for catalogue.py. No network, no build: catalogues and checkouts are made in a temp dir.

    python3 -m unittest discover -s corpus/scripts -p 'test_*.py'
"""
import argparse
import contextlib
import importlib.util
import io
import json
import os
import re
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("catalogue", HERE / "catalogue.py")
catalogue = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(catalogue)


def git(d, *args):
    return subprocess.run(['git', '-C', str(d), *args], check=True, capture_output=True,
                          text=True).stdout.strip()


class CatalogueTest(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        root = Path(os.path.realpath(self._tmp.name))
        self.public = root / 'public'
        self.private = root / 'private'
        self.oss = root / 'test-oss'
        for d in (self.public, self.private, self.oss):
            d.mkdir()
        self._env = {k: os.environ.get(k) for k in ('CORPUS_CATALOGUE', 'TEST_OSS_ROOT')}
        os.environ['CORPUS_CATALOGUE'] = f'{self.public}:{self.private}'
        os.environ['TEST_OSS_ROOT'] = str(self.oss)

    def tearDown(self):
        for k, v in self._env.items():
            if v is None:
                os.environ.pop(k, None)
            else:
                os.environ[k] = v
        self._tmp.cleanup()

    def entry(self, where, name, text):
        f = where / f'{name}.yml'
        f.write_text(textwrap.dedent(text))
        return f

    def checkout(self, name, commits=2):
        d = self.oss / name
        d.mkdir()
        git(d, 'init', '-q')
        shas = []
        for i in range(commits):
            (d / 'f').write_text(str(i))
            git(d, 'add', 'f')
            git(d, '-c', 'user.name=t', '-c', 'user.email=t@t', 'commit', '-qm', str(i))
            shas.append(git(d, 'rev-parse', 'HEAD'))
        return d, shas


class TestOverlay(CatalogueTest):

    def test_extends_merges_by_field_and_records_origins(self):
        pub = self.entry(self.public, 'vavr', '''
            name: vavr
            status: active
            config:
              route: maven-plugin
              module: vavr
              baseline: baselines/vavr.tsv
            ''')
        priv = self.entry(self.private, 'vavr', '''
            extends: vavr
            config:
              mem: 12G
              baseline: baselines/vavr-private.tsv
            diagnostics:
              baseline: baselines/vavr-diagnostics.tsv
            ''')
        e = catalogue.load_one('vavr')
        self.assertEqual({'route': 'maven-plugin', 'module': 'vavr', 'mem': '12G',
                          'baseline': 'baselines/vavr-private.tsv'}, e['config'])
        self.assertEqual(str(pub), str(catalogue.origin_of(e, 'config.module')))
        self.assertEqual(str(priv), str(catalogue.origin_of(e, 'config.mem')))
        self.assertEqual(str(priv), str(catalogue.origin_of(e, 'diagnostics.baseline')))
        # the baseline follows the file that declared it, not the entry's first file
        self.assertEqual(self.private / 'baselines' / 'vavr-private.tsv', catalogue.baseline_path(e))
        self.assertIsNone(catalogue.record_refusal(e))

    def test_a_scalar_or_list_replaces_whole_and_forgets_origins_below(self):
        self.entry(self.public, 'p', '''
            build:
              provides: [a, b]
            ''')
        priv = self.entry(self.private, 'p', '''
            extends: p
            build: none-here
            ''')
        e = catalogue.load_one('p')
        self.assertEqual('none-here', e['build'])
        self.assertNotIn(('build', 'provides'), e['_origin'])
        self.assertEqual(str(priv), str(catalogue.origin_of(e, 'build.provides')))

    def test_an_unmarked_name_collision_is_an_error(self):
        self.entry(self.public, 'p', 'status: active\n')
        self.entry(self.private, 'p', 'status: dormant\n')
        with self.assertRaises(SystemExit) as cm:
            catalogue.load_all()
        self.assertIn('extends: p', str(cm.exception))

    def test_replaces_keeps_the_whole_entry_override(self):
        self.entry(self.public, 'p', 'status: active\nsummary: public\n')
        self.entry(self.private, 'p', 'replaces: true\nstatus: dormant\n')
        e = catalogue.load_one('p')
        self.assertEqual('dormant', e['status'])
        self.assertNotIn('summary', e)

    def test_extends_needs_an_earlier_entry(self):
        self.entry(self.private, 'ghost', 'extends: ghost\nstatus: active\n')
        with self.assertRaises(SystemExit):
            catalogue.load_all()

    def test_recording_a_public_baseline_under_a_private_overlay_is_refused(self):
        self.entry(self.public, 'vavr', '''
            config:
              baseline: baselines/vavr.tsv
            ''')
        self.entry(self.private, 'vavr', '''
            extends: vavr
            parse:
              config: inputConfiguration.private.json
            ''')
        e = catalogue.load_one('vavr')
        self.assertEqual(self.public / 'baselines' / 'vavr.tsv', catalogue.baseline_path(e))
        self.assertIn('Declare config.baseline in the overlay', catalogue.record_refusal(e))
        # and the refusal happens before anything is measured or written
        self.assertEqual(1, catalogue.baseline_cmd(e, record=True))
        self.assertFalse((self.public / 'baselines').exists())


class TestPinning(CatalogueTest):

    def test_rev_state_and_check_rev(self):
        d, (first, second) = self.checkout('lib')
        self.entry(self.public, 'lib', f'''
            source:
              kind: git
              url: file://{d}
              rev: {first}
            ''')
        e = catalogue.load_one('lib')
        r = catalogue.rev_state(e)
        self.assertEqual((second, False), (r['head'], r['at_pin']))
        self.assertEqual(1, catalogue.check_rev(e))
        git(d, 'checkout', '-q', first)
        self.assertTrue(catalogue.rev_state(e)['at_pin'])
        self.assertEqual(0, catalogue.check_rev(e))
        # untracked files of ours do not count as dirty; a tracked edit does
        (d / 'inputConfiguration.json').write_text('{}')
        self.assertFalse(catalogue.rev_state(e)['dirty'])
        (d / 'f').write_text('edited')
        self.assertTrue(catalogue.rev_state(e)['dirty'])

    def test_a_shared_checkout_is_pinned_by_its_owner(self):
        d, (first, _) = self.checkout('lib')
        self.entry(self.public, 'lib', f'''
            source:
              kind: git
              url: file://{d}
              rev: {first}
            ''')
        self.entry(self.public, 'lib-plugin', 'dir: lib\n')
        e = catalogue.load_one('lib-plugin')
        r = catalogue.rev_state(e)
        self.assertEqual(('lib', first, False), (r['owner'], r['pinned'], r['at_pin']))

    def test_unpinned_passes_check_rev_but_not_when_a_pin_is_required(self):
        d, _ = self.checkout('lib')
        self.entry(self.public, 'lib', f'source:\n  kind: git\n  url: file://{d}\n')
        e = catalogue.load_one('lib')
        self.assertEqual(0, catalogue.check_rev(e))
        self.assertEqual(1, catalogue.check_rev(e, require_pin=True))

    def test_pin_inserts_after_url_and_keeps_comments(self):
        d, (_, head) = self.checkout('lib')
        f = self.entry(self.public, 'lib', f'''
            name: lib
            source:
              kind: git
              url: file://{d}
              # a comment that must survive
            build:
              cmd: make
            ''')
        self.assertEqual(0, catalogue.pin(catalogue.load_one('lib')))
        text = f.read_text()
        self.assertIn(f'  url: file://{d}\n  rev: {head}\n  # a comment that must survive\n', text)
        self.assertEqual(head, catalogue.load_one('lib')['source']['rev'])

    def test_pin_replaces_an_existing_rev(self):
        d, (first, head) = self.checkout('lib')
        f = self.entry(self.public, 'lib', f'''
            source:
              kind: git
              url: file://{d}
              rev: {first}
            ''')
        self.assertEqual(0, catalogue.pin(catalogue.load_one('lib')))
        self.assertEqual(1, f.read_text().count('rev:'))
        self.assertEqual(head, catalogue.load_one('lib')['source']['rev'])

    def test_a_private_source_is_pinned_in_the_private_file(self):
        d, (_, head) = self.checkout('lib')
        pub = self.entry(self.public, 'lib', 'status: active\n')
        priv = self.entry(self.private, 'lib', f'''
            extends: lib
            source:
              kind: git
              url: file://{d}
            ''')
        before = pub.read_text()
        self.assertEqual(0, catalogue.pin(catalogue.load_one('lib')))
        self.assertEqual(before, pub.read_text())
        self.assertIn(f'rev: {head}', priv.read_text())

    def test_obtain_clones_and_checks_out_the_pin(self):
        upstream, (first, _) = self.checkout('upstream')
        self.entry(self.public, 'lib', f'''
            source:
              kind: git
              url: file://{upstream}
              rev: {first}
            ''')
        e = catalogue.load_one('lib')
        self.assertEqual(0, catalogue.obtain(e))
        self.assertEqual(first, git(self.oss / 'lib', 'rev-parse', 'HEAD'))
        self.assertTrue(catalogue.rev_state(e)['at_pin'])

    def test_obtain_refuses_to_check_out_over_tracked_edits(self):
        d, (first, _) = self.checkout('lib')
        self.entry(self.public, 'lib', f'''
            source:
              kind: git
              url: file://{d}
              rev: {first}
            ''')
        (d / 'f').write_text('edited')
        self.assertEqual(1, catalogue.obtain(catalogue.load_one('lib')))
        self.assertEqual('edited', (d / 'f').read_text())


class TestMachineProfile(CatalogueTest):

    def setUp(self):
        super().setUp()
        self.machines = self.private.parent / 'machines'
        self.machines.mkdir()
        for k in ('CORPUS_MACHINES', 'CORPUS_HOST', 'BUILD_JAVA_HOME'):
            self._env.setdefault(k, os.environ.get(k))
            os.environ.pop(k, None)
        os.environ['CORPUS_MACHINES'] = str(self.machines)
        os.environ['CORPUS_HOST'] = 'box'

    def profile(self, text, host='box'):
        (self.machines / f'{host}.yml').write_text(textwrap.dedent(text))

    def fake_jdk(self, version):
        home = self.private.parent / f'jdk{version}'
        (home / 'bin').mkdir(parents=True)
        java = home / 'bin' / 'java'
        java.write_text(f'#!/bin/sh\necho "    java.specification.version = {version}" >&2\n')
        java.chmod(0o755)
        return home

    def doctor(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            rc = catalogue.cmd_doctor(argparse.Namespace(names=[]))
        return rc, out.getvalue()

    def test_no_profile_changes_nothing(self):
        os.environ['CORPUS_HOST'] = 'elsewhere'
        self.assertIsNone(catalogue.machine_profile())
        self.entry(self.public, 'gone', 'status: active\n')
        rc, out = self.doctor()
        self.assertEqual(0, rc)
        self.assertIn('no machine profile for elsewhere', out)

    def test_a_held_entry_that_is_absent_fails_doctor_and_a_skipped_one_does_not(self):
        self.entry(self.public, 'gone', 'status: active\n')
        self.entry(self.public, 'big', 'status: active\n')
        self.entry(self.public, 'old', 'status: dormant\n')
        self.profile('''
            host: box
            holds: active
            skip:
              big: too large for this disk
            ''')
        rc, out = self.doctor()
        self.assertEqual(1, rc)
        self.assertRegex(out, r'gone .*!! HELD HERE; COPY-ONLY')
        self.assertRegex(out, r'big .*not held here: too large for this disk')
        self.assertRegex(out, r'old .*not held here')

    def test_holds_can_be_a_list(self):
        self.entry(self.public, 'a', 'status: active\n')
        self.entry(self.public, 'b', 'status: active\n')
        self.profile('holds: [a]\n')
        p = catalogue.machine_profile()
        self.assertEqual((True, None), catalogue.expected_here(catalogue.load_one('a'), p))
        self.assertEqual((False, None), catalogue.expected_here(catalogue.load_one('b'), p))

    def test_a_profile_for_another_host_is_refused(self):
        self.profile('host: other\n')
        with self.assertRaises(SystemExit):
            catalogue.machine_profile()

    def test_build_runs_on_the_profiles_jdk_for_build_jdk_version(self):
        self.entry(self.public, 'old', '''
            build:
              cmd: ./gradlew build
              jdk: {version: 21}
            ''')
        self.entry(self.public, 'new', 'build:\n  cmd: ./gradlew build\n')
        self.profile('jdks:\n  - {version: 21, home: /opt/jdk21}\n')
        self.assertEqual('JAVA_HOME=/opt/jdk21 ./gradlew build',
                         catalogue.plan(catalogue.load_one('old'), 'build'))
        self.assertEqual('./gradlew build', catalogue.plan(catalogue.load_one('new'), 'build'))
        os.environ['BUILD_JAVA_HOME'] = '/opt/override'
        self.assertEqual('JAVA_HOME=/opt/override ./gradlew build',
                         catalogue.plan(catalogue.load_one('old'), 'build'))

    def test_check_jdk_uses_the_profiles_jdk(self):
        home = self.fake_jdk(21)
        self.entry(self.public, 'old', 'build:\n  cmd: make\n  jdk: {version: 21}\n')
        self.profile(f'jdks:\n  - {{version: 21, home: {home}}}\n')
        self.assertEqual(0, catalogue.check_jdk(catalogue.load_one('old')))

    def test_machine_checks_jdks_root_and_names(self):
        good, bad = self.fake_jdk(21), self.fake_jdk(17)
        osname = {'Darwin': 'macos', 'Linux': 'linux'}[os.uname().sysname]
        self.entry(self.public, 'a', 'status: active\n')
        self.profile(f'''
            os: {osname}
            test_oss_root: {self.oss}
            jdks:
              - {{version: 21, home: {good}}}
              - {{version: 25, home: {bad}}}
            skip:
              nosuch: typo
            ''')
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            rc = catalogue.cmd_machine(argparse.Namespace(init=False))
        self.assertEqual(1, rc)
        text = out.getvalue()
        self.assertRegex(text, r'jdk  21  \S+jdk21\n')
        self.assertIn('!! reports 17', text)
        self.assertIn("!! names 'nosuch'", text)
        self.assertIn('holds 1 of 1 entries: a', text)


class TestPhases(CatalogueTest):

    def test_parse_reads_parse_config_when_given(self):
        self.entry(self.public, 'vavr', '''
            config:
              route: maven-plugin
            parse:
              config: inputConfiguration.main.json
              steps: [prep, modification]
            ''')
        e = catalogue.load_one('vavr')
        want = self.oss / 'vavr' / 'inputConfiguration.main.json'
        self.assertEqual(want, catalogue.parse_config_path(e))
        self.assertIn(f'--input-configuration={want} --analysis-steps=none',
                      catalogue.plan(e, 'parse'))

    def test_analyse_runs_parse_steps_when_there_is_no_test(self):
        self.entry(self.public, 'a', 'parse:\n  steps: [prep, modification]\n')
        self.entry(self.public, 'b', 'parse:\n  test: TestB\n  steps: [prep]\n')
        self.entry(self.public, 'c', 'parse:\n  runner: openjdk\n')
        self.assertIn('--analysis-steps=prep,modification',
                      catalogue.plan(catalogue.load_one('a'), 'analyse'))
        self.assertIn("slowTest --tests '*TestB'", catalogue.plan(catalogue.load_one('b'), 'analyse'))
        self.assertIsNone(catalogue.plan(catalogue.load_one('c'), 'analyse'))

    def test_config_then_follows_the_route_in_order(self):
        self.entry(self.public, 'v', 'config:\n  route: maven-plugin\n  module: v\n  then:\n'
                                     '    - python3 {scripts}/derive.py .\n    - mvn install\n')
        cmd = catalogue.plan(catalogue.load_one('v'), 'config')
        self.assertTrue(cmd.startswith('( mkdir -p '), cmd)
        # {scripts} is the scripts directory of the catalogue that declared the entry, NOT maddi's own:
        # a private catalogue directory has to be able to keep its scripts beside itself. This assertion
        # named catalogue.HERE until that changed; what it is really testing -- that `then` runs after
        # the route, in order -- is the same either way.
        scripts = self.public.parent / 'scripts'
        self.assertTrue(cmd.endswith(f') && python3 {scripts}/derive.py . && mvn install'), cmd)

    def test_reactor_jars_are_rewritten_to_output_dirs_and_m2_is_left_alone(self):
        d = self.oss / 'guava'
        d.mkdir()
        m2 = '/home/u/.m2/repository/x/x/1/x-1.jar'
        (d / 'compile.javac.log').write_text(
            f'[DEBUG] -d {d}/guava-testlib/target/classes -classpath {d}/guava/target/guava-33.jar:'
            f'{d}/guava/target/guava-33-tests.jar:{m2}\n')
        p = subprocess.run(['bash', '-c', catalogue._REWRITE_REACTOR_JARS + 'true'], cwd=d,
                           capture_output=True, text=True)
        self.assertEqual(0, p.returncode, p.stderr)
        self.assertIn('rewrote 2 reactor-jar classpath entries', p.stdout)
        self.assertEqual(f'[DEBUG] -d {d}/guava-testlib/target/classes -classpath {d}/guava/target/classes:'
                         f'{d}/guava/target/test-classes:{m2}\n', (d / 'compile.javac.log').read_text())

    def test_the_maven_log_route_carries_rewrite_and_maddi_args(self):
        self.entry(self.public, 'g', 'config:\n  route: maven-log\n  tasks: install -DskipTests\n'
                                     '  rewrite_reactor_jars: true\n  maddi_args: --jre /opt/jdk17\n')
        cmd = catalogue.plan(catalogue.load_one('g'), 'config')
        self.assertIn('compile.javac.log > compile.javac.log.tmp', cmd)
        self.assertIn('/compile.javac.log --jre /opt/jdk17 --write-input-configuration', cmd)

    def test_plan_exits_2_for_a_phase_the_entry_does_not_have(self):
        """⛔ THE NUMBER IS A CONTRACT. Taskfile.yml's `_cat` treats exit 2 from `plan` as "this corpus
        has no such phase, skip it and go on", which is what lets `corpus:ready` run over guava, detekt
        and caffeine -- none of which has a `build:`, because their config route IS the build. Change
        this code and those composites start failing on a phase that was never supposed to exist."""
        self.entry(self.public, 'routeless', 'config:\n  route: none\n')
        args = argparse.Namespace(name='routeless', phase='build')
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            self.assertEqual(2, catalogue.cmd_plan(args))
        self.assertIn('no build phase defined', err.getvalue())
        # `route: none` is the same statement about the config phase
        self.assertEqual(2, catalogue.cmd_plan(argparse.Namespace(name='routeless', phase='config')))
        # and a phase that IS defined still exits 0
        self.entry(self.public, 'buildable', 'build:\n  cmd: make\n')
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            self.assertEqual(0, catalogue.cmd_plan(argparse.Namespace(name='buildable', phase='build')))
        self.assertEqual('make', out.getvalue().strip())

    def overlay(self, name, entry_text, script_name=None, script_text='# ours\n'):
        """A catalogue directory shaped like the real ones: <root>/catalogue beside <root>/scripts, so
        what the test asserts is what a private overlay actually looks like on disk."""
        root = Path(os.path.realpath(self._tmp.name)) / name
        cat, scripts = root / 'catalogue', root / 'scripts'
        cat.mkdir(parents=True), scripts.mkdir(parents=True)
        os.environ['CORPUS_CATALOGUE'] = f"{self.public}:{cat}"
        script = None
        if script_name:
            script = scripts / script_name
            script.write_text(script_text)
        return cat, script

    def test_a_config_script_is_resolved_against_the_catalogue_that_declared_it(self):
        """⛔ THE POINT OF CORPUS_CATALOGUE BEING A LIST. A private entry that needs a hand-written
        config script keeps that script beside itself. Resolving against maddi's own scripts directory
        meant putting it in the PUBLIC repository."""
        cat, script = self.overlay('devops', None, script_name='private-config.py')
        self.entry(cat, 'customer', 'config:\n  route: script\n'
                                    '  script: ../scripts/private-config.py\n')
        e = catalogue.load_one('customer')
        self.assertEqual(script, catalogue.script_path(e))
        self.assertIn(f'python3 {script}', catalogue.plan(e, 'config'))

    def test_the_then_placeholder_follows_the_declaring_catalogue_too(self):
        cat, script = self.overlay('devops2', None, script_name='derive.py')
        self.entry(cat, 'over', 'config:\n  route: maven-plugin\n  module: m\n'
                                '  then:\n    - python3 {scripts}/derive.py .\n')
        self.assertIn(f'python3 {script} .', catalogue.plan(catalogue.load_one('over'), 'config'))

    def test_a_missing_config_script_is_named_rather_than_handed_to_the_shell(self):
        """`python3 <nonexistent>` fails as "can't open file", once the phase is already running -- the
        symptom, not the cause."""
        cat, _ = self.overlay('devops3', None)
        self.entry(cat, 'gone', 'config:\n  route: script\n  script: ../scripts/not-here.py\n')
        err = io.StringIO()
        with contextlib.redirect_stderr(err), self.assertRaises(SystemExit) as raised:
            catalogue.script_path(catalogue.load_one('gone'))
        self.assertIn('not-here.py', str(raised.exception))

    def test_baseline_if_declared_passes_without_a_baseline(self):
        self.entry(self.public, 'n', 'parse:\n  runner: openjdk\n')
        e = catalogue.load_one('n')
        self.assertEqual(1, catalogue.baseline_cmd(e, record=False))
        self.assertEqual(0, catalogue.baseline_cmd(e, record=False, if_declared=True))

    def test_a_kotlin_entry_refuses_parse_only_and_baseline(self):
        self.entry(self.public, 'k', 'config:\n  baseline: b.tsv\nparse:\n  runner: kotlin\n  test: TestK\n')
        e = catalogue.load_one('k')
        with self.assertRaises(SystemExit) as cm:
            catalogue.plan(e, 'parse')
        self.assertIn('no parse-only mode', str(cm.exception))
        self.assertEqual(1, catalogue.baseline_cmd(e, record=False))
        self.assertIn("slowTest --tests '*TestK'", catalogue.plan(e, 'analyse'))

    def test_vendor_skips_a_configuration_outside_the_corpus(self):
        out = self.private.parent / 'server-work' / 'build.inputConfiguration.json'
        out.parent.mkdir()
        out.write_text('{}')
        self.entry(self.private, 'priv', f'dir: {self.private.parent}\nconfig:\n  output: {out}\n')
        self.assertEqual(0, catalogue.vendor(catalogue.load_one('priv')))

    def test_plugin_routes_default_to_the_checkouts_version(self):
        """`_cat` never exported MADDI_PLUGIN_VERSION, so the plugin coordinate came out versionless."""
        saved = os.environ.pop('MADDI_PLUGIN_VERSION', None)
        try:
            self.entry(self.public, 'g', 'config:\n  route: gradle-plugin\n')
            want = re.search(r'^version=(\S+)', (HERE.parent.parent / 'gradle.properties').read_text(),
                             re.M).group(1)
            self.assertIn(f'-Dmaddi.pluginVersion={want} ', catalogue.plan(catalogue.load_one('g'), 'config'))
            os.environ['MADDI_PLUGIN_VERSION'] = '9.9'
            self.assertIn('-Dmaddi.pluginVersion=9.9 ', catalogue.plan(catalogue.load_one('g'), 'config'))
        finally:
            os.environ.pop('MADDI_PLUGIN_VERSION', None)
            if saved is not None:
                os.environ['MADDI_PLUGIN_VERSION'] = saved

    def test_the_kotlin_route_greps_for_what_ParseKotlincList_parses(self):
        """The route once grepped `[KOTLIN] compiler arguments:`, which no Gradle log contains."""
        java = (HERE.parent.parent / 'maddi-run-kotlin/src/main/java/io/codelaser/maddi/run/kotlinmain'
                / 'kotlinc/ParseKotlincList.java').read_text()
        pattern = re.search(r'GRADLE_PATTERN = "\.\*(.+?)\\\\s\*', java).group(1)
        self.entry(self.public, 'k', 'config:\n  route: gradle-log-kotlin\n  tasks: classes\n')
        cmd = catalogue.plan(catalogue.load_one('k'), 'config')
        grep = re.search(r"grep -aE '([^']+)'", cmd).group(1)
        self.assertIn(pattern, grep.split('|'))
        # and what the grep keeps, the parser matches
        line = f'10:00 [DEBUG] [org.gradle.api.Task] :p:compileKotlin v: {pattern} -d out a.kt'
        self.assertTrue(re.search(grep, line))
        self.assertTrue(re.fullmatch(r'.*' + re.escape(pattern) + r'\s*(.+)', line))



class EngineWorkspaceTest(CatalogueTest):
    """CatalogueTest, plus an isolated engine workspace: `registered` and `register` read
    $REFACTOR_HOME, and a test that fell through to the real ~/refactorhome would be answering about
    this machine instead of about the fixture."""

    def setUp(self):
        super().setUp()
        self.home = Path(os.path.realpath(self._tmp.name)) / 'refactorhome'
        self._prev_home = os.environ.get('REFACTOR_HOME')
        os.environ['REFACTOR_HOME'] = str(self.home)

    def tearDown(self):
        if self._prev_home is None:
            os.environ.pop('REFACTOR_HOME', None)
        else:
            os.environ['REFACTOR_HOME'] = self._prev_home
        super().tearDown()

    def config(self, name, *source_sets):
        f = self.oss / name / 'inputConfiguration.json'
        f.write_text(json.dumps({'sourceSets': [{'name': s} for s in source_sets]}))
        return f


def read_project_yml(path):
    """Read a generated project.yml with the same reader catalogue.py uses. The point of these tests is
    that the ENGINE's YAML reader will accept the file, so parsing it is closer to that than matching
    the text line by line."""
    return catalogue.parse_yaml(path.read_text(), str(path))


class TestClean(EngineWorkspaceTest):

    def test_it_discards_edits_deletes_generated_sources_and_returns_to_the_pin(self):
        d, shas = self.checkout('lib', commits=2)
        self.entry(self.public, 'lib', """
            source:
              kind: git
              url: file://%s
              rev: %s
              generated:
                - target/generated-sources
            """ % (d, shas[0]))
        gen = d / 'target' / 'generated-sources'
        gen.mkdir(parents=True)
        (gen / 'Made.java').write_text('class Made {}')
        (d / 'f').write_text('an edit an earlier run left behind')
        self.assertEqual(shas[1], git(d, 'rev-parse', 'HEAD'))

        self.assertEqual(0, catalogue.clean(catalogue.load_one('lib')))

        self.assertFalse(gen.exists(), 'generated sources are untracked: only clean deletes them')
        self.assertEqual('0', (d / 'f').read_text(), 'the edit must be gone')
        self.assertEqual(shas[0], git(d, 'rev-parse', 'HEAD'), 'clean moves HEAD to source.rev')

    def test_our_own_output_survives_unless_also_ours_is_asked_for(self):
        d, shas = self.checkout('lib')
        self.entry(self.public, 'lib', """
            source: {kind: git, url: "file://%s", rev: %s}
            config:
              route: maven-log
              tasks: test-compile
            """ % (d, shas[0]))
        ours = ('inputConfiguration.json', 'compile.log', 'compile.javac.log')
        for f in ours:
            (d / f).write_text('ours')

        # Untracked files are not dirt: a CONFIGURED corpus must still come out of clean green.
        self.assertEqual(0, catalogue.clean(catalogue.load_one('lib')))
        self.assertTrue((d / 'inputConfiguration.json').is_file(),
                        'remaking the configuration is the expensive phase; clean keeps it')

        self.assertEqual(0, catalogue.clean(catalogue.load_one('lib'), also_ours=True))
        for f in ours:
            self.assertFalse((d / f).exists(), f + ' is ours, and --also-ours deletes it')

    def test_it_refuses_a_pattern_or_a_path_that_leaves_the_checkout(self):
        d, shas = self.checkout('lib')
        outside = self.oss / 'not-the-corpus'
        outside.mkdir()
        (outside / 'keep').write_text('untouched')
        self.entry(self.public, 'lib',
                   'source: {kind: git, url: "file://%s", rev: %s}\n' % (d, shas[0]))
        for generated in ('../not-the-corpus', 'target/*', '/etc'):
            with self.subTest(generated=generated):
                e = catalogue.load_one('lib')
                e['source']['generated'] = [generated]
                self.assertEqual(1, catalogue.clean(e))
        self.assertTrue((outside / 'keep').is_file(), 'nothing outside the checkout may be deleted')

    def test_an_absent_or_non_git_directory_is_refused_rather_than_emptied(self):
        self.entry(self.public, 'gone', 'source: {kind: git, url: "file:///nowhere"}\n')
        self.assertEqual(1, catalogue.clean(catalogue.load_one('gone')))

        plain = self.oss / 'plain'
        plain.mkdir()
        (plain / 'file').write_text('not a checkout')
        self.entry(self.public, 'plain', 'source: {kind: git, url: "file:///nowhere"}\n')
        self.assertEqual(1, catalogue.clean(catalogue.load_one('plain')))
        self.assertTrue((plain / 'file').is_file())


class TestRegister(EngineWorkspaceTest):

    def entry_with_config(self, name='lib', extra=''):
        d, shas = self.checkout(name)
        self.entry(self.public, name, """
            source: {kind: git, url: "file://%s", rev: %s}
            config:
              route: maven-log
              tasks: test-compile
            tests:
              cmd: ./mvnw test
            %s
            """ % (d, shas[0], extra))
        # A Maven source-set name with a colon AND a space in it -- guava's real shape.
        self.config(name, 'core/main', 'Guava: Google Core Libraries for Java/test')
        # `checkout` leaves HEAD on its LAST commit while the entry pins the first, and register
        # refuses an off-pin checkout -- so put the fixture where the entry says it is.
        git(d, 'checkout', '-q', '--detach', shas[0])
        return d, shas

    def test_it_links_the_checkout_and_the_one_configuration_and_writes_the_source_sets(self):
        d, shas = self.entry_with_config()
        (d / 'pom.xml').write_text('<project/>')
        e = catalogue.load_one('lib')
        self.assertEqual(0, catalogue.register(e))

        link = self.home / 'projects' / 'lib'
        cfg_link = self.home / 'work' / 'lib' / 'build.inputConfiguration.json'
        self.assertEqual(d.resolve(), link.resolve())
        self.assertEqual((d / 'inputConfiguration.json').resolve(), cfg_link.resolve())
        self.assertTrue(cfg_link.is_symlink(), 'linked, never copied: one configuration must exist')

        got = read_project_yml(self.home / 'work' / 'lib' / 'project.yml')
        self.assertEqual(str(d.resolve()), got['build']['directory'])
        self.assertEqual(str((d / 'pom.xml').resolve()), got['build']['monitor'])
        gitdir = got['build']['git_dirs'][str((d / '.git').resolve())]
        self.assertEqual(shas[0], gitdir['baseRevision'])
        self.assertEqual('main', gitdir['branch'], 'no origin/HEAD in a local clone -> main')
        self.assertEqual(['core/main', 'Guava: Google Core Libraries for Java/test'],
                         gitdir['sourceSets'], 'a colon in a name must survive as ONE scalar')
        self.assertEqual('./mvnw test', got['test']['run_tests_command'])
        # The engine must not be able to build or configure a corpus -- that is the other way round.
        self.assertNotIn('compile_debug_command', got['build'])
        self.assertNotIn('clean_command', got['build'])
        self.assertTrue(catalogue.registered(e))

    def test_doctor_does_not_claim_an_unconfigured_corpus_is_configured(self):
        """A build-only entry has no configuration and cannot be registered, and both are normal. The
        note said "configured but the engine cannot load it" beside a `config` column reading False --
        two statements in one row contradicting each other."""
        d, shas = self.checkout('lib')
        self.entry(self.public, 'lib', """
            source: {kind: git, url: "file://%s", rev: %s}
            build:
              cmd: make
            """ % (d, shas[0]))
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            catalogue.cmd_doctor(argparse.Namespace(names=['lib']))
        row = [l for l in out.getvalue().splitlines() if l.startswith('lib')][0]
        self.assertNotIn('configured but', row, row)

    def test_doctor_does_say_so_when_a_configured_corpus_is_not_registered(self):
        d, shas = self.entry_with_config()
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            catalogue.cmd_doctor(argparse.Namespace(names=['lib']))
        row = [l for l in out.getvalue().splitlines() if l.startswith('lib')][0]
        self.assertIn('configured but the engine cannot load it', row, row)
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(0, catalogue.register(catalogue.load_one('lib')))
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            catalogue.cmd_doctor(argparse.Namespace(names=['lib']))
        row = [l for l in out.getvalue().splitlines() if l.startswith('lib')][0]
        self.assertNotIn('configured but', row, row)
        self.assertIn('2 source sets', row, row)

    def test_a_route_that_produced_no_configuration_is_a_failure_and_names_the_file(self):
        """The entry says how to configure itself, so a missing configuration means the route did not
        run or did not work -- and registering would hide that."""
        d, shas = self.checkout('lib')
        self.entry(self.public, 'lib', """
            source: {kind: git, url: "file://%s", rev: %s}
            config:
              route: maven-log
              tasks: test-compile
            """ % (d, shas[0]))
        e = catalogue.load_one('lib')
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            self.assertEqual(1, catalogue.register(e))
        self.assertIn(str(d / 'inputConfiguration.json'), err.getvalue())
        self.assertFalse((self.home / 'projects' / 'lib').exists(),
                         'a refused registration leaves nothing half-made')
        self.assertFalse(catalogue.registered(e))

    def test_an_entry_with_no_config_phase_is_not_a_failure_and_is_simply_not_registered(self):
        """⛔ THE OTHER HALF OF THE SAME QUESTION. A build-only entry (or `route: none`) is never going
        to have a configuration, so there is nothing for the engine to read and nothing went wrong --
        `corpus:ready` has to walk past it. Reporting 1 here stopped the whole chain one step after the
        config phase itself had correctly been skipped."""
        d, shas = self.checkout('lib')
        self.entry(self.public, 'lib', """
            source: {kind: git, url: "file://%s", rev: %s}
            build:
              cmd: make
            """ % (d, shas[0]))
        e = catalogue.load_one('lib')
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            self.assertEqual(0, catalogue.register(e))
            self.assertEqual(0, catalogue.vendor(e))
        self.assertIn('nothing for the engine to load', err.getvalue())
        self.assertIn('no jars to vendor', err.getvalue())
        self.assertFalse(catalogue.registered(e), 'not a failure, but not registered either')

    def test_a_checkout_off_its_pin_is_refused_before_anything_is_written(self):
        """baseRevision comes from source.rev, and the engine RESETS a project to it -- so registering
        a checkout that sits elsewhere writes a configuration for a tree that is not there."""
        d, shas = self.entry_with_config()
        git(d, 'checkout', '-q', '--detach', shas[-1])
        e = catalogue.load_one('lib')
        self.assertNotEqual(shas[0], git(d, 'rev-parse', 'HEAD'))
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            self.assertEqual(1, catalogue.register(e))
        self.assertIn(shas[0][:12], err.getvalue())
        self.assertFalse((self.home / 'projects' / 'lib').exists())
        self.assertFalse(catalogue.registered(e))

    def test_a_configuration_with_no_source_sets_is_refused(self):
        d, shas = self.checkout('lib')
        self.entry(self.public, 'lib',
                   'source: {kind: git, url: "file://%s", rev: %s}\n' % (d, shas[0]))
        self.config('lib')
        self.assertEqual(1, catalogue.register(catalogue.load_one('lib')))

    def test_a_link_left_pointing_elsewhere_is_not_registered(self):
        d, shas = self.entry_with_config()
        e = catalogue.load_one('lib')
        self.assertEqual(0, catalogue.register(e))

        other, _ = self.checkout('other')
        link = self.home / 'projects' / 'lib'
        link.unlink()
        link.symlink_to(other.resolve())
        self.assertFalse(catalogue.registered(e),
                         'a stale link makes the engine load the WRONG corpus and report nothing')
        self.assertEqual(0, catalogue.register(e))
        self.assertTrue(catalogue.registered(e), 'registering again repairs it')

    def test_a_copied_configuration_is_moved_aside_and_replaced_by_the_link(self):
        d, shas = self.entry_with_config()
        work = self.home / 'work' / 'lib'
        work.mkdir(parents=True)
        (work / 'build.inputConfiguration.json').write_text('{"sourceSets": []} a stale copy')
        self.assertEqual(0, catalogue.register(catalogue.load_one('lib')))
        self.assertTrue((work / 'build.inputConfiguration.json').is_symlink())
        self.assertIn('a stale copy',
                      (work / 'build.inputConfiguration.json.was-a-copy').read_text())

    def test_a_real_directory_under_projects_is_never_replaced(self):
        d, shas = self.entry_with_config()
        real = self.home / 'projects' / 'lib'
        real.mkdir(parents=True)
        (real / 'someones-work').write_text('not ours to delete')
        self.assertEqual(1, catalogue.register(catalogue.load_one('lib')))
        self.assertTrue((real / 'someones-work').is_file())

    def test_engine_project_names_the_registration_so_two_entries_can_share_one_checkout(self):
        d, shas = self.entry_with_config(
            extra='engine:\n              project: lib-plugin\n              branch: devel')
        e = catalogue.load_one('lib')
        self.assertEqual('lib-plugin', catalogue.engine_project(e))
        self.assertEqual(0, catalogue.register(e))
        self.assertEqual(d.resolve(), (self.home / 'projects' / 'lib-plugin').resolve())
        got = read_project_yml(self.home / 'work' / 'lib-plugin' / 'project.yml')
        self.assertEqual('devel',
                         got['build']['git_dirs'][str((d / '.git').resolve())]['branch'])


if __name__ == '__main__':
    unittest.main()
