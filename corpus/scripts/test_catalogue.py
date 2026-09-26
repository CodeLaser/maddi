#!/usr/bin/env python3
"""
Tests for catalogue.py. No network, no build: catalogues and checkouts are made in a temp dir.

    python3 -m unittest discover -s corpus/scripts -p 'test_*.py'
"""
import importlib.util
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


if __name__ == '__main__':
    unittest.main()
