#!/usr/bin/env python3
"""
Tests for vendor-libraries.py. No network: downloads go through an injected fetch.

    python3 -m unittest discover -s corpus/scripts -p 'test_*.py'
"""
import hashlib
import importlib.util
import io
import json
import os
import tempfile
import unittest
import urllib.error

_spec = importlib.util.spec_from_file_location(
    "vendor_libraries", os.path.join(os.path.dirname(os.path.abspath(__file__)), "vendor-libraries.py"))
vendor_libraries = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(vendor_libraries)

LOMBOK = b"lombok bytes"
LOMBOK_SHA1 = hashlib.sha1(LOMBOK).hexdigest()


class TestVendorLibraries(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.home = os.path.realpath(self._tmp.name)
        self.corpus = os.path.join(self.home, "git", "test-oss")
        self.modules = os.path.join(self.home, ".gradle", "caches", "modules-2", "files-2.1")
        self.m2 = os.path.join(self.home, ".m2", "repository")
        self.fetched = []

    def tearDown(self):
        self._tmp.cleanup()

    def write(self, path, data):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
        return path

    def gradle_jar(self, group, artifact, version, data, present=True):
        digest = hashlib.sha1(data).hexdigest()
        path = os.path.join(self.modules, group, artifact, version, digest, f"{artifact}-{version}.jar")
        if present:
            self.write(path, data)
        return path

    def config(self, project, paths, pretty=True, below=""):
        parts = [{"name": "java.base", "uri": "jmod:java.base"}]
        parts += [{"name": os.path.basename(p), "uri": "file:" + p, "library": True} for p in paths]
        d = {"workingDirectory": os.path.join(self.corpus, project), "classPathParts": parts}
        path = os.path.join(self.corpus, project, below, "inputConfiguration.json")
        # the two spellings the generators produce: Jackson's pretty printer and a compact one
        text = json.dumps(d, indent=2, separators=(",", " : ")) if pretty else json.dumps(d)
        self.write(path, text.encode())
        return path

    def fetch(self, url):
        self.fetched.append(url)
        table = {
            f"{vendor_libraries.CENTRAL}/org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar": LOMBOK,
            f"{vendor_libraries.CENTRAL}/org/yaml/snakeyaml/2.6/snakeyaml-2.6.jar": b"tampered",
            f"{vendor_libraries.CENTRAL}/commons-io/commons-io/2.11/commons-io-2.11.jar": b"cio",
            f"{vendor_libraries.CENTRAL}/commons-io/commons-io/2.11/commons-io-2.11.jar.sha1":
                (hashlib.sha1(b"cio").hexdigest() + "  commons-io-2.11.jar\n").encode(),
        }
        table.update(getattr(self, "table_extra", {}))
        if url not in table:
            raise urllib.error.HTTPError(url, 404, "not found", None, None)
        return table[url]

    def vendor(self, *configs, **kw):
        v = vendor_libraries.Vendor(self.corpus, fetch=self.fetch, out=io.StringIO(), home=self.home, **kw)
        ok = all([v.vendor(c) for c in configs])
        return v, ok

    def uris(self, config):
        with open(config) as f:
            return [p["uri"] for p in json.load(f)["classPathParts"]]

    def lib(self, project, *relative):
        return os.path.join(self.corpus, "lib", project, *relative)

    def test_gradle_and_maven_jars_move_into_one_maven_layout(self):
        guava = self.gradle_jar("com.google.guava", "guava", "33.0", b"guava")
        junit = self.write(os.path.join(self.m2, "junit/junit/4.13/junit-4.13.jar"), b"junit")
        inside = os.path.join(self.corpus, "p", "build", "classes")
        cfg = self.config("p", [guava, junit, inside])
        _, ok = self.vendor(cfg)
        self.assertTrue(ok)
        self.assertEqual(["jmod:java.base",
                          "file:" + self.lib("p", "com/google/guava/guava/33.0/guava-33.0.jar"),
                          "file:" + self.lib("p", "junit/junit/4.13/junit-4.13.jar"),
                          "file:" + inside], self.uris(cfg))
        with open(self.lib("p", "com/google/guava/guava/33.0/guava-33.0.jar"), "rb") as f:
            self.assertEqual(b"guava", f.read())

    def test_only_uris_change_and_the_formatting_is_kept(self):
        guava = self.gradle_jar("g", "a", "1", b"x")
        for pretty in (True, False):
            cfg = self.config("fmt" + str(pretty), [guava], pretty=pretty)
            with open(cfg) as f:
                before = f.read()
            self.vendor(cfg)
            with open(cfg) as f:
                after = f.read()
            new = "file:" + self.lib("fmt" + str(pretty), "g/a/1/a-1.jar")
            self.assertEqual(before.replace(json.dumps("file:" + guava), json.dumps(new)), after)

    def test_a_second_run_changes_nothing(self):
        cfg = self.config("p", [self.gradle_jar("g", "a", "1", b"x")])
        self.vendor(cfg)
        with open(cfg) as f:
            once = f.read()
        v, ok = self.vendor(cfg)
        with open(cfg) as f:
            self.assertEqual(once, f.read())
        self.assertTrue(ok)
        self.assertEqual(0, v.stats["copied"] + v.stats["linked"] + v.stats["downloaded"])

    def test_the_configuration_is_backed_up_before_the_rewrite(self):
        cfg = self.config("p", [self.gradle_jar("g", "a", "1", b"x")], below="sub/build")
        with open(cfg) as f:
            original = f.read()
        self.vendor(cfg)
        backups = os.listdir(self.lib("p", "_backup"))
        self.assertEqual(1, len(backups))
        self.assertTrue(backups[0].startswith("p__sub__build__inputConfiguration.json."))
        with open(self.lib("p", "_backup", backups[0])) as f:
            self.assertEqual(original, f.read())

    def test_a_jar_gone_from_the_gradle_cache_is_downloaded_and_checked_against_its_directory(self):
        gone = os.path.join(self.modules, "org.projectlombok", "lombok", "1.18.42", LOMBOK_SHA1,
                            "lombok-1.18.42.jar")
        cfg = self.config("p", [gone])
        v, ok = self.vendor(cfg)
        self.assertTrue(ok)
        self.assertEqual(1, v.stats["downloaded"])
        with open(self.lib("p", "org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar"), "rb") as f:
            self.assertEqual(LOMBOK, f.read())

    def test_a_download_with_the_wrong_digest_is_refused_and_the_old_path_kept(self):
        gone = os.path.join(self.modules, "org.yaml", "snakeyaml", "2.6", "0" * 40, "snakeyaml-2.6.jar")
        cfg = self.config("p", [gone])
        v, ok = self.vendor(cfg)
        self.assertFalse(ok)
        self.assertEqual(1, v.stats["unrecovered"])
        self.assertEqual("file:" + gone, self.uris(cfg)[1])
        self.assertFalse(os.path.exists(self.lib("p", "org/yaml/snakeyaml/2.6/snakeyaml-2.6.jar")))

    def test_a_jar_gone_from_m2_is_checked_against_centrals_sha1(self):
        gone = os.path.join(self.m2, "commons-io/commons-io/2.11/commons-io-2.11.jar")
        cfg = self.config("p", [gone])
        _, ok = self.vendor(cfg)
        self.assertTrue(ok)
        self.assertEqual("file:" + self.lib("p", "commons-io/commons-io/2.11/commons-io-2.11.jar"),
                         self.uris(cfg)[1])

    def test_a_missing_snapshot_is_never_looked_for_on_central(self):
        gone = os.path.join(self.m2, "org/x/y/1.0-SNAPSHOT/y-1.0-SNAPSHOT.jar")
        cfg = self.config("p", [gone])
        _, ok = self.vendor(cfg)
        self.assertFalse(ok)
        self.assertEqual([], self.fetched)

    def test_offline_downloads_nothing(self):
        gone = os.path.join(self.modules, "org.projectlombok", "lombok", "1.18.42", LOMBOK_SHA1,
                            "lombok-1.18.42.jar")
        _, ok = self.vendor(self.config("p", [gone]), offline=True)
        self.assertFalse(ok)
        self.assertEqual([], self.fetched)

    def test_dry_run_copies_and_rewrites_nothing(self):
        cfg = self.config("p", [self.gradle_jar("g", "a", "1", b"x")])
        with open(cfg) as f:
            before = f.read()
        v, _ = self.vendor(cfg, dry_run=True)
        with open(cfg) as f:
            self.assertEqual(before, f.read())
        self.assertFalse(os.path.exists(self.lib("p")))
        self.assertEqual(1, v.stats["copied"])

    def test_the_same_jar_in_a_second_project_is_hard_linked(self):
        jar = self.gradle_jar("g", "a", "1", b"shared")
        self.vendor(self.config("one", [jar]), self.config("two", [jar]))
        first, second = self.lib("one", "g/a/1/a-1.jar"), self.lib("two", "g/a/1/a-1.jar")
        self.assertEqual(os.stat(first).st_ino, os.stat(second).st_ino)

    def test_gradle_transforms_keep_their_hash_directory(self):
        patched = self.write(os.path.join(self.home, ".gradle", "caches", "9.6.1", "transforms", "0f3a",
                                          "transformed", "azure-core-1.55.3-patched.jar"), b"patched")
        cfg = self.config("p", [patched])
        self.vendor(cfg)
        self.assertEqual("file:" + self.lib("p", "_gradle-transforms", "0f3a", "azure-core-1.55.3-patched.jar"),
                         self.uris(cfg)[1])

    def test_a_different_file_already_at_the_target_is_an_error(self):
        cfg = self.config("p", [self.gradle_jar("g", "a", "1", b"one")])
        self.write(self.lib("p", "g/a/1/a-1.jar"), b"another")
        with self.assertRaises(ValueError):
            self.vendor(cfg)

    def test_a_digest_directory_without_its_leading_zero_still_checks(self):
        # Gradle drops leading zeros: 0e3f... is stored as e3f..., 39 characters
        data = next(b"aop" + bytes([i]) for i in range(256) if hashlib.sha1(b"aop" + bytes([i])).hexdigest()[0] == "0")
        digest = hashlib.sha1(data).hexdigest()
        self.table_extra = {f"{vendor_libraries.CENTRAL}/aopalliance/aopalliance/1.0/aopalliance-1.0.jar": data}
        gone = os.path.join(self.modules, "aopalliance", "aopalliance", "1.0", digest.lstrip("0"), "aopalliance-1.0.jar")
        cfg = self.config("p", [gone])
        v, ok = self.vendor(cfg)
        self.assertTrue(ok)
        self.assertEqual(1, v.stats["downloaded"])

    def test_a_configuration_from_another_machine_is_skipped_untouched(self):
        mine = self.gradle_jar("g", "a", "1", b"x")
        cfg = self.config("p", [mine, "/Users/somebody/.m2/repository/g/b/1/b-1.jar"])
        with open(cfg) as f:
            before = f.read()
        v, ok = self.vendor(cfg)
        self.assertTrue(ok)
        with open(cfg) as f:
            self.assertEqual(before, f.read())
        self.assertEqual(1, v.stats["foreign"])
        self.assertEqual([], self.fetched)

    def test_all_finds_module_configurations_and_skips_lib(self):
        jar = self.gradle_jar("g", "a", "1", b"x")
        top = self.config("p", [jar])
        sub = self.config("p", [jar], below="core/target")
        self.vendor(top)
        self.assertEqual(sorted([top, sub]), vendor_libraries.all_configs(self.corpus))


if __name__ == "__main__":
    unittest.main()
