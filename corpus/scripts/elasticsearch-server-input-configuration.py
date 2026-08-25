#!/usr/bin/env python3
"""
Generate the checked-in, machine-independent input configuration for the elasticsearch
*server* slice: maddi-run-openjdk/src/test/resources/corpus/elasticsearch-server.json.

Run via `task config:elasticsearch-server`, after `task elasticsearch` has built the corpus
and `task config:elasticsearch` has captured the whole-reactor configuration.

WHY A SLICE, AND WHY CHECKED IN
-------------------------------
TestElasticsearchServer wants "the elasticsearch server sources" — the large-method stress
corpus. `config:elasticsearch` captures the whole Gradle reactor: 521 source sets, most of
them x-pack and qa modules that have nothing to do with that, and 27 parsing/inspection
errors' worth of exotica along with them. server/main alone is the workload the test names.

The generated whole-reactor file also cannot be checked in: it pins 560 class-path jars by
absolute path into ~/.gradle/caches, and those paths rot (see PluginSourceSets.isModularArtifact
— 537 of 537 gone on the two modular corpora). So this slice keeps ONLY paths inside the
corpus checkout, and writes them RELATIVE to workingDirectory. The result is the same file on
every machine, and `TestOssCorpus.ROOT` is what varies instead.

WHAT IS IN THE SLICE
--------------------
  source:     server/main                        (server/src/main/java)
  class path: the 19 libs/*/main it depends on, as their build/classes/java/main directories,
              plus the JDK jmods.

THE 26 EXTERNAL JARS ARE NAMED AS COORDINATES, NOT PATHS
--------------------------------------------------------
They cannot simply be dropped. maddi tolerates an incomplete class path for its own resolution
(unresolved references degrade to warnings) but JAVAC DOES NOT: attributing server/main with
lucene absent dies inside the compiler itself, `NullPointerException ... "t1" is null` at
com.sun.tools.javac.code.Types.sideCast, validating a generic supertype it cannot see. Measured
2026-08-25; the run aborts before maddi's own tolerance is ever consulted.

Nor can their paths be checked in: they live at
  <gradle home>/caches/modules-2/files-2.1/<group>/<artifact>/<version>/<sha1>/<file>.jar
where both the home and the sha1 are machine-specific.

So this writes them as `gradle-cache:<group>/<artifact>/<version>/<file>.jar`, dropping the sha1,
and TestElasticsearchServer globs that one directory level at run time. The coordinate is the
part that is true on every machine; the digest is not.

EVERY PATH HERE IS RELATIVE TO THE CORPUS ROOT, AND workingDirectory IS "."
--------------------------------------------------------------------------
The reader does NOT accept this file as it stands, and that is deliberate rather than an
oversight to fix here. ComputeSourceSets.absoluteURI states the invariant: a class-path part
must carry an ABSOLUTE hierarchical file URI, because the openjdk inspector does a bare
`Path.of(classPathPart.uri())` with no working-directory resolution, and `Path.of` throws
"URI is not hierarchical" on the opaque `file:<relative>` form. Only SOURCE DIRECTORIES are
resolved against workingDirectory.

That invariant is fine for the paths it was written about - Gradle-cache jars, machine-specific
either way - but this slice's class path is INSIDE the corpus, where relative does buy
portability. So the committed artifact stays relative and TestElasticsearchServer absolutizes it
against TestOssCorpus.dir("elasticsearch") into a temp file before handing it to Main. The file
you review and diff is machine-independent; the file maddi reads satisfies its own contract.
"""
import json
import os
import pathlib
import sys

CORPUS = "elasticsearch"
ROOT_SOURCE_SET = "server/main"


def main() -> int:
    oss_root = pathlib.Path(os.environ.get("TEST_OSS_ROOT")
                            or (pathlib.Path.home() / "git" / "test-oss"))
    corpus_root = oss_root / CORPUS
    full = corpus_root / "inputConfiguration.json"
    if not full.is_file():
        print(f"missing {full}; run `task elasticsearch && task config:elasticsearch` first",
              file=sys.stderr)
        return 1
    d = json.loads(full.read_text())
    by_name = {s["name"]: s for s in d["sourceSets"]}

    # transitive closure of the root source set over source-set dependencies; anything that is
    # not itself a source set is an external jar name, and is dropped (see the module docstring).
    closure, stack = set(), [ROOT_SOURCE_SET]
    while stack:
        n = stack.pop()
        if n in closure or n not in by_name:
            continue
        closure.add(n)
        stack.extend(by_name[n].get("dependencies", []))
    libs = sorted(closure - {ROOT_SOURCE_SET})

    abs_root = str(corpus_root)

    def relative(p: str) -> str:
        """A path inside the corpus, relative to workingDirectory; anything else is refused."""
        p = p.removeprefix("file:")
        while p.startswith("//"):
            p = p[1:]
        if not p.startswith(abs_root + "/"):
            raise ValueError(f"outside the corpus, cannot be made relative: {p}")
        return p[len(abs_root) + 1:]

    # class path: the JDK modules exactly as captured, then one entry per lib, named as the
    # dependency names in server/main so they resolve by name on the way back in.
    class_path = [c for c in d["classPathParts"] if c.get("uri", "").startswith("jmod:")]

    # the external jars this closure names, as coordinates; see the module docstring
    CACHE = "/caches/modules-2/files-2.1/"
    by_uri = {c["name"]: c.get("uri", "") for c in d["classPathParts"]}
    jar_names = sorted({dep for n in closure for dep in by_name[n].get("dependencies", [])
                        if dep not in by_name})
    unresolved = []
    for jar in jar_names:
        uri = by_uri.get(jar, "")
        if CACHE in uri:
            # an external artifact: keep the coordinate, drop the machine-specific sha1 directory
            group, artifact, version, _sha1, filename = uri.split(CACHE, 1)[1].split("/")
            new_uri = f"gradle-cache:{group}/{artifact}/{version}/{filename}"
        elif uri.startswith("file:") and abs_root in uri:
            # a project whose classes the capture put on the class path rather than in a source set
            # (libs/native/main is one): inside the corpus, so it relativizes like the libs above
            new_uri = "file:" + relative(uri)
        else:
            unresolved.append(jar)
            continue
        class_path.append({
            "sourceEncoding": "UTF-8",
            "name": jar,
            "uri": new_uri,
            "library": True,
            "externalLibrary": True,
            "module": True,
            "restrictToPackages": [],
        })
    for name in libs:
        s = by_name[name]
        class_path.append({
            "sourceEncoding": s.get("sourceEncoding", "UTF-8"),
            "name": name,
            "uri": "file:" + relative(s["uri"]),
            "library": True,
            "externalLibrary": True,
            "module": bool(s.get("module")),
            "restrictToPackages": [],
        })

    server = by_name[ROOT_SOURCE_SET]
    kept = {c["name"] for c in class_path}
    source_set = {
        "sourceEncoding": server.get("sourceEncoding", "UTF-8"),
        "name": ROOT_SOURCE_SET,
        "buildUnit": relative(server["buildUnit"]),
        "sourceDirectories": [relative(p) for p in server["sourceDirectories"]],
        "uri": "file:" + relative(server["uri"]),
        "module": bool(server.get("module")),
        "restrictToPackages": [],
        # only what survives: an unknown dependency name is a WARN and a silent drop on the way in
        "dependencies": [n for n in server.get("dependencies", []) if n in kept],
    }

    out_dir = pathlib.Path(__file__).resolve().parent.parent.parent \
        / "maddi-run-openjdk" / "src" / "test" / "resources" / "corpus"
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / "elasticsearch-server.json"
    out.write_text(json.dumps({
        # every path below is relative to the corpus root; the test resolves them
        "workingDirectory": ".",
        "classPathParts": class_path,
        "sourceSets": [source_set],
        "alternativeJREDirectory": None,
    }, indent=1) + "\n")
    jmods = sum(1 for c in class_path if c["uri"].startswith("jmod:"))
    jars = sum(1 for c in class_path if c["uri"].startswith("gradle-cache:"))
    dirs = sum(1 for c in class_path if c["uri"].startswith("file:")) - len(libs)
    print(f"wrote {out}")
    print(f"  source sets {len(d['sourceSets'])} -> 1 ({ROOT_SOURCE_SET}), class path "
          f"{len(d['classPathParts'])} -> {len(class_path)} "
          f"({jmods} jmod + {len(libs)} libs + {dirs} class dirs + {jars} jars as coordinates)")
    if unresolved:
        # stated, never silent: a name with no cache path is a class-path entry this slice LOSES
        print(f"  WARNING: {len(unresolved)} dependency name(s) had no Gradle-cache path and were "
              f"dropped: {', '.join(unresolved[:6])}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
