#!/usr/bin/env python3
"""
Copy every jar an inputConfiguration.json names from OUTSIDE the corpus into
<corpus>/lib/<project>/, and point the configuration there.

    vendor-libraries.py [--corpus ROOT] [--dry-run] [--offline] CONFIG...
    vendor-libraries.py [--corpus ROOT] [--dry-run] [--offline] --all
    vendor-libraries.py [--corpus ROOT] [--dry-run] [--offline] --coordinates --project NAME CONFIG...

Run by every `_config:*` task after it writes a configuration; run it by hand once on a machine
whose configurations predate it. Idempotent: a path already inside the corpus is left alone, so a
second run changes nothing.

WHY
---
A generated configuration names its class path by absolute path, and for most corpora that means
the build tool's caches: ~/.gradle/caches/modules-2 and ~/.m2/repository. Neither is ours to keep.
Gradle deletes a cache entry it has not itself used for 30 days, and it cannot see that a
configuration still names it -- on 2026-09-25 that took 18 jars out from under three corpora in one
cleanup. ~/.m2 is never cleaned automatically but is cleared by hand, and some of what it holds
(locally installed SNAPSHOTs) cannot be downloaded from anywhere. Gradle's artifact transforms
(caches/<version>/transforms/..., e.g. a `-patched.jar`) cannot be re-derived without the build.

So each corpus gets its own copy, next to the corpus and out of every cache's reach.

LAYOUT OF lib/<project>/
------------------------
  <group as path>/<artifact>/<version>/<file>   Gradle modules-2 AND ~/.m2 -- one Maven layout,
                                                 so a jar found in both caches is one file here
  _gradle-transforms/<hash>/<file>               Gradle artifact transforms
  _other/<digest of the old path>/<file>         anything else outside the corpus
  _backup/<config path>.<timestamp>              each configuration, before its first rewrite

<project> is the first path component of the configuration below the corpus root, so
pulsar/pulsar-client-api/build/inputConfiguration.json vendors into lib/pulsar/. A jar that another
project's lib/ already holds with the same content is HARD-LINKED, not copied: elasticsearch and
elasticsearch-server share most of theirs.

A JAR THAT IS ALREADY GONE
--------------------------
is downloaded from Maven Central (unless --offline). For a Gradle module the cache directory
above the jar IS the jar's SHA-1, so the download is checked against the digest the configuration
itself recorded. For ~/.m2 it is checked against Central's own .sha1. A jar that cannot be
recovered keeps its old path, is named, and makes the exit status 1.

A CONFIGURATION THAT NAMES COORDINATES
--------------------------------------
A committed configuration cannot carry this machine's paths, so some name a Gradle-cache jar by
coordinate instead: `gradle-cache:<group>/<artifact>/<version>/<file>` (maddi-mod's
elasticsearch-server.json, written by elasticsearch-server-input-configuration.py). Its reader looks in
lib/<project>/<group as path>/<artifact>/<version>/<file> first and in the Gradle cache second, so
`--coordinates --project <project>` fills exactly that place -- from the Gradle cache, else ~/.m2, else
Maven Central against Central's .sha1 -- and leaves the configuration alone: it is portable by design.
Without this the cache cleanup took 22 of its 25 jars and TestElasticsearchServer failed (2026-10-02).

The rewrite is textual -- each old `uri` string replaced by the new one -- so the configuration
keeps whatever formatting its generator gave it, and a diff shows only the paths that moved.

LOMBOK IS REPLACED BY 1.18.48
-----------------------------
Whatever Lombok a configuration names (org.projectlombok:lombok, the annotation processor), it is
vendored as lombok-1.18.48.jar instead: the copy, the `uri` and every reference to the jar's
`name` (the source sets' dependencies) all say 1.18.48. Also for a configuration vendored before,
whose Lombok already sits in lib/.

JDK 27 removed com.sun.tools.javac.tree.EndPosTable, and Lombok up to 1.18.46 needs it: its
processor dies with an ExceptionInInitializerError, and maddi then records a parse error for every
source set that uses Lombok (pulsar declares 1.18.42: 105 source sets; timefold-solver and
dolphinscheduler 1.18.46). 1.18.48 runs on JDK 24, 26 and 27. These corpora are pinned upstream
checkouts, and their own builds cannot compile on JDK 27 with the Lombok they declare either, so
parsing with it would reproduce nothing; for what they use (@Slf4j, @Getter, builders, ...) 1.18.48
generates the same members. This changes test data, not maddi: maddi parses with the Lombok a
configuration names, and says so when that Lombok cannot run.

The jar is taken from the Gradle cache or ~/.m2 when either holds it, else downloaded from Maven
Central (unless --offline); either way it must have the SHA-1 pinned below. Not touched:
rewrite-java-lombok-*.jar, an OpenRewrite artifact rather than the processor. Nor a configuration
read with --coordinates, which this script never rewrites; none of those names Lombok.
"""
import argparse
import glob
import hashlib
import json
import os
import re
import shutil
import sys
import time
import urllib.error
import urllib.request

CENTRAL = "https://repo1.maven.org/maven2"
MODULES = "/caches/modules-2/files-2.1/"
M2 = "/.m2/repository/"
LIB = "lib"

LOMBOK_VERSION = "1.18.48"
LOMBOK_SHA1 = "6858f13541bab505384f07053c5a7b539bbfd3e3"  # repo1.maven.org's .sha1, and Gradle's cache directory
LOMBOK_RELATIVE = f"org/projectlombok/lombok/{LOMBOK_VERSION}/lombok-{LOMBOK_VERSION}.jar"
# the processor jar, lombok-<version>.jar; not rewrite-java-lombok-*.jar, not lombok-<version>-sources.jar
LOMBOK_JAR = re.compile(r"^lombok-\d+(\.\d+)*(-SNAPSHOT)?\.jar$")


def sha1(path):
    h = hashlib.sha1()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def strip_scheme(uri):
    """'file:/x' -> '/x', 'file:///x' -> '/x'; None for anything that is not a file URI."""
    if not uri.startswith("file:"):
        return None
    p = uri[len("file:"):]
    while p.startswith("//"):
        p = p[1:]
    return p if p.startswith("/") else None


class Location:
    """Where a jar outside the corpus goes under lib/<project>/, and how to get it back if it is gone."""

    def __init__(self, relative, expected_sha1=None, central=None):
        self.relative = relative            # below lib/<project>/
        self.expected_sha1 = expected_sha1  # known digest, or None
        self.central = central              # path below Maven Central, or None


def locate(path):
    if MODULES in path:
        parts = path.split(MODULES, 1)[1].split("/")
        if len(parts) == 5:
            group, artifact, version, digest, name = parts
            relative = "/".join([group.replace(".", "/"), artifact, version, name])
            # Gradle writes the digest without its leading zeros: .../aopalliance/1.0/235ba8b4...e8/ is 39
            return Location(relative, digest.rjust(40, "0"), relative)
    if M2 in path:
        relative = path.split(M2, 1)[1]
        # a locally installed SNAPSHOT has no counterpart on Central
        return Location(relative, None, None if "-SNAPSHOT" in relative else relative)
    if "/transforms/" in path and "/.gradle" in path:
        after = path.split("/transforms/", 1)[1].split("/")
        # caches/<v>/transforms/<hash>/transformed/<file>, or caches/transforms-N/<hash>/...
        return Location("/".join(["_gradle-transforms", after[0], after[-1]]))
    digest = hashlib.sha1(path.encode()).hexdigest()[:16]
    return Location("/".join(["_other", digest, os.path.basename(path)]))


def default_fetch(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()


class Vendor:

    def __init__(self, corpus, dry_run=False, offline=False, fetch=default_fetch, out=sys.stdout,
                 home=os.path.expanduser("~")):
        self.corpus = os.path.realpath(corpus)
        self.home = os.path.realpath(home)
        self.lib_root = os.path.join(self.corpus, LIB)
        self.dry_run = dry_run
        self.offline = offline
        self.fetch = fetch
        self.out = out
        self.stats = {"copied": 0, "linked": 0, "downloaded": 0, "present": 0, "unrecovered": 0,
                      "lombok": 0}

    def say(self, msg):
        print(msg, file=self.out)

    def project_of(self, config):
        rel = os.path.relpath(os.path.realpath(config), self.corpus)
        if rel.startswith("..") or rel.split(os.sep)[0] == LIB:
            raise ValueError(f"{config} is not inside a project of the corpus {self.corpus}")
        return rel.split(os.sep)[0], rel

    def inside_corpus(self, path):
        return os.path.realpath(path).startswith(self.corpus + os.sep)

    def twin(self, project, relative, digest):
        """The same file with the same content in another project's lib/, to hard-link from."""
        for other in sorted(glob.glob(os.path.join(self.lib_root, "*", relative))):
            if not other.startswith(os.path.join(self.lib_root, project) + os.sep) \
                    and sha1(other) == digest:
                return other
        return None

    def recover(self, location):
        """-> bytes of a jar no longer in the cache, verified; None if it cannot be had."""
        if self.offline or not location.central:
            return None
        try:
            data = self.fetch(f"{CENTRAL}/{location.central}")
            expected = location.expected_sha1 \
                or self.fetch(f"{CENTRAL}/{location.central}.sha1").decode().split()[0].strip()
        except (urllib.error.URLError, OSError, ValueError, IndexError):
            return None
        return data if hashlib.sha1(data).hexdigest() == expected else None

    def place(self, project, source, location):
        """Put one jar at lib/<project>/<relative>. -> its new absolute path, or None if unrecovered."""
        target = os.path.join(self.lib_root, project, location.relative)
        exists = os.path.exists(source)
        if exists and os.path.isdir(source):
            # not seen in any corpus so far; copied whole rather than refused
            if not os.path.exists(target):
                self.stats["copied"] += 1
                if not self.dry_run:
                    shutil.copytree(source, target)
            else:
                self.stats["present"] += 1
            return target
        digest = sha1(source) if exists else location.expected_sha1
        if os.path.isfile(target):
            if digest is None or sha1(target) == digest:
                self.stats["present"] += 1
                return target
            if not (exists and "-SNAPSHOT" in location.relative):
                raise ValueError(f"{target} already holds a different file than {source}")
            # A SNAPSHOT is re-installed by every build of its reactor (jenkins' cli), so different
            # bytes under the same name are the newer build, not a mix-up: replace it below.
            self.say(f"  refreshed   {location.relative}")
        if exists:
            twin = self.twin(project, location.relative, digest)
            self.stats["linked" if twin else "copied"] += 1
            if not self.dry_run:
                os.makedirs(os.path.dirname(target), exist_ok=True)
                tmp = target + ".part"
                if twin:
                    os.link(twin, tmp)
                else:
                    shutil.copy2(source, tmp)
                os.replace(tmp, target)
            return target
        data = self.recover(location)
        if data is None:
            self.stats["unrecovered"] += 1
            self.say(f"  UNRECOVERED {source}" + ("" if location.central else " (not on Maven Central)"))
            return None
        self.stats["downloaded"] += 1
        self.say(f"  downloaded  {location.central}")
        if not self.dry_run:
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target + ".part", "wb") as f:
                f.write(data)
            os.replace(target + ".part", target)
        return target

    def lombok_source(self):
        """A local lombok-1.18.48.jar with the pinned digest, or a path that does not exist (place() downloads)."""
        candidates = sorted(glob.glob(os.path.join(self.home, ".gradle", MODULES.strip("/"), "org.projectlombok",
                                                   "lombok", LOMBOK_VERSION, "*", f"lombok-{LOMBOK_VERSION}.jar")))
        candidates.append(os.path.join(self.home, M2.strip("/"), LOMBOK_RELATIVE))
        for candidate in candidates:
            if os.path.isfile(candidate) and sha1(candidate) == LOMBOK_SHA1:
                return candidate
        return os.path.join(self.home, M2.strip("/"), LOMBOK_RELATIVE)  # absent: recovered from Central

    def vendor(self, config):
        """-> True when every class-path entry of this configuration is now inside the corpus."""
        project, rel = self.project_of(config)
        with open(config, encoding="utf-8") as f:
            text = f.read()
        external, lombok = [], {}
        for part in json.loads(text).get("classPathParts", []):
            uri = part.get("uri", "")
            path = strip_scheme(uri)
            name = os.path.basename(path) if path is not None else None
            if name and LOMBOK_JAR.match(name) and name != f"lombok-{LOMBOK_VERSION}.jar":
                lombok[uri] = part.get("name", name)  # replaced, inside the corpus or not -- see the docstring
                if not self.inside_corpus(path):
                    external.append(uri)  # still counts for the another-machine check below
            elif path is not None and not self.inside_corpus(path) and uri not in external:
                external.append(uri)
        foreign = [u for u in external if not strip_scheme(u).startswith(self.home + "/")]
        if foreign:
            # Generated under another machine's home (a /Users/... file on Linux): it never worked
            # here, and half-rewriting it would only disguise that. Regenerate it on this machine.
            self.say(f"{rel}: SKIPPED, {len(foreign)} path(s) outside {self.home}, e.g. "
                     f"{strip_scheme(foreign[0])} -- generated on another machine; regenerate it here")
            self.stats["foreign"] = self.stats.get("foreign", 0) + 1
            return True
        moves, renames, complete = {}, {}, True
        for uri in external:
            if uri in lombok:
                continue
            path = strip_scheme(uri)
            new = self.place(project, path, locate(path))
            if new is None:
                complete = False
            else:
                moves[uri] = "file:" + new
        if lombok:
            names = {p.get("name") for p in json.loads(text).get("classPathParts", [])}
            if f"lombok-{LOMBOK_VERSION}.jar" in names:
                raise ValueError(f"{config}: names lombok-{LOMBOK_VERSION}.jar AND {sorted(set(lombok.values()))};"
                                 f" replacing would leave two class-path parts with one name")
            new = self.place(project, self.lombok_source(), Location(LOMBOK_RELATIVE, LOMBOK_SHA1, LOMBOK_RELATIVE))
            if new is None:
                complete = False
            else:
                self.stats["lombok"] += 1
                for uri, name in lombok.items():
                    moves[uri] = "file:" + new
                    renames[name] = f"lombok-{LOMBOK_VERSION}.jar"
                self.say(f"{rel}: Lombok {sorted(set(lombok.values()))} -> lombok-{LOMBOK_VERSION}.jar")
        if moves:
            # json.dumps gives the exact spelling the generator wrote: both are plain JSON strings. A uri and a
            # name never share a spelling ('"file:/...jar"' vs '"lombok-1.18.42.jar"'), so the order is immaterial.
            for old, new in list(moves.items()) + list(renames.items()):
                text = text.replace(json.dumps(old), json.dumps(new))
            check = {p.get("uri") for p in json.loads(text).get("classPathParts", [])}
            missing = [old for old in moves if old in check]
            if missing:
                raise ValueError(f"{config}: could not rewrite {len(missing)} uri(s), e.g. {missing[0]}")
            if any(json.dumps(name) in text for name in renames):
                raise ValueError(f"{config}: a reference to {sorted(renames)} survived the rewrite")
            if not self.dry_run:
                backup = os.path.join(self.lib_root, project, "_backup",
                                      rel.replace(os.sep, "__") + time.strftime(".%Y%m%d-%H%M%S"))
                os.makedirs(os.path.dirname(backup), exist_ok=True)
                shutil.copy2(config, backup)
                tmp = config + ".vendor-tmp"
                with open(tmp, "w", encoding="utf-8") as f:
                    f.write(text)
                shutil.copymode(config, tmp)
                os.replace(tmp, config)
        self.say(f"{rel}: {len(moves)} path(s) moved into {LIB}/{project}/"
                 + ("" if complete else " -- INCOMPLETE, see UNRECOVERED above"))
        return complete


GRADLE_CACHE = "gradle-cache:"


def vendor_coordinates(v, config, project):
    """-> True when every gradle-cache: entry of `config` is in lib/<project>/. Never rewrites `config`."""
    with open(config, encoding="utf-8") as f:
        d = json.load(f)
    coordinates = []
    for field in ("classPathParts", "sourceSets"):
        for part in d.get(field, []):
            uri = part.get("uri", "")
            if uri.startswith(GRADLE_CACHE) and uri not in coordinates:
                coordinates.append(uri)
    complete, bad = True, [c for c in coordinates if len(c[len(GRADLE_CACHE):].split("/")) != 4]
    if bad:
        raise ValueError(f"{config}: not <group>/<artifact>/<version>/<file>: {bad[0]}")
    for uri in coordinates:
        group, artifact, version, name = uri[len(GRADLE_CACHE):].split("/")
        relative = "/".join([group.replace(".", "/"), artifact, version, name])
        cached = sorted(glob.glob(os.path.join(v.home, ".gradle", MODULES.strip("/"), group, artifact,
                                               version, "*", name)))
        m2 = os.path.join(v.home, M2.strip("/"), relative)
        if cached:
            source, location = cached[0], locate(cached[0])
        elif os.path.isfile(m2):
            source, location = m2, locate(m2)
        else:
            # gone from both caches: `uri` is what UNRECOVERED names, and Central's .sha1 checks it
            source, location = uri, Location(relative, None, None if "-SNAPSHOT" in relative else relative)
        if v.place(project, source, location) is None:
            complete = False
    v.say(f"{os.path.basename(config)}: {len(coordinates)} coordinate(s) in {LIB}/{project}/"
          + ("" if complete else " -- INCOMPLETE, see UNRECOVERED above"))
    return complete


def all_configs(corpus):
    found = set()
    for pattern in ("*/inputConfiguration.json", "*/*/target/inputConfiguration.json",
                    "*/*/build/inputConfiguration.json"):
        found.update(glob.glob(os.path.join(corpus, pattern)))
    return sorted(c for c in found if os.path.relpath(c, corpus).split(os.sep)[0] != LIB)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("configs", nargs="*")
    ap.add_argument("--all", action="store_true", help="every configuration in the corpus")
    ap.add_argument("--corpus", default=os.environ.get("TEST_OSS_ROOT")
                    or os.path.expanduser("~/git/test-oss"))
    ap.add_argument("--dry-run", action="store_true", help="report, copy nothing, rewrite nothing")
    ap.add_argument("--offline", action="store_true", help="never download a jar that is gone")
    ap.add_argument("--coordinates", action="store_true",
                    help="CONFIG names gradle-cache: coordinates; fill lib/<--project>/, never rewrite CONFIG")
    ap.add_argument("--project", help="with --coordinates: the lib/<project>/ the configuration's reader looks in")
    a = ap.parse_args(argv)
    if a.coordinates:
        if a.all or not a.configs or not a.project:
            ap.error("--coordinates takes --project NAME and the configuration(s), not --all")
        v = Vendor(a.corpus, a.dry_run, a.offline)
        ok = all([vendor_coordinates(v, c, a.project) for c in a.configs])
        s = v.stats
        print(f"{'(dry run) ' if a.dry_run else ''}{len(a.configs)} configuration(s): "
              f"{s['copied']} copied, {s['linked']} hard-linked, {s['downloaded']} downloaded, "
              f"{s['present']} already in {LIB}/, {s['unrecovered']} unrecovered")
        return 0 if ok else 1
    configs = all_configs(a.corpus) if a.all else a.configs
    if not configs:
        ap.error("name the configuration(s), or --all")
    v = Vendor(a.corpus, a.dry_run, a.offline)
    ok = all([v.vendor(c) for c in configs])
    s = v.stats
    print(f"{'(dry run) ' if a.dry_run else ''}{len(configs)} configuration(s): "
          f"{s['copied']} copied, {s['linked']} hard-linked, {s['downloaded']} downloaded, "
          f"{s['present']} already in {LIB}/, {s['unrecovered']} unrecovered, "
          f"{s['lombok']} with Lombok replaced by {LOMBOK_VERSION}"
          + (f"; {s['foreign']} configuration(s) from another machine skipped" if s.get("foreign") else ""))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
