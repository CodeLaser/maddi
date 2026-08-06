#!/usr/bin/env python3
"""
Generate <TEST_OSS_ROOT>/coil/inputConfiguration.json — the JVM slice of coil-core.

Run via `task config:coil`.

WHY THIS IS A SCRIPT AND NOT ONE OF THE TWO NORMAL ROUTES
--------------------------------------------------------
Every other corpus config here comes from either the maddi Maven plugin or a `--compile-log`
capture. Coil is Kotlin Multiplatform, and neither route reaches it:

  * the Gradle plugin keys on the `org.jetbrains.kotlin.jvm` plugin, the java plugin's `SourceSet`
    container and the `compileKotlin` task — a multiplatform build has none of those (its sets live
    under `kotlin.sourceSets`, and the task is `compileKotlinJvm`);
  * `--compile-log` needs coil's build to run, and coil applies the Android Gradle plugin, so
    configuration fails outright without an Android SDK.

So the configuration is assembled directly, the way maddi's own `TestKotlinStdlibParse` assembles
the one for kotlin-stdlib's sources.

WHAT THE SLICE IS
-----------------
coil-core's JVM target *main* source sets, flattened into ONE maddi source set — which is what a
`compileKotlinJvm` invocation would itself yield, since the hierarchy source sets have no compile of
their own. All six are required: `commonMain` holds `expect` declarations whose `actual`s live in
the others.

Android, JS, wasmJs and native are deliberately excluded. maddi's K2 session is built on
`JvmPlatforms.defaultJvmPlatform`, and keeping several targets would give one FQN several `actual`
declarations.

The JDK is not listed: both KotlinInspector and MixedProjectInspector take jdkHome from the running
JVM's `java.home`, and the mixed driver adds `jmod:java.base` itself.
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile

# coil-core's JVM target main source sets, hierarchy order (most common first).
SOURCE_SETS = ["commonMain", "nonAndroidMain", "nonJsCommonMain", "nonAppleMain",
               "jvmCommonMain", "jvmMain"]

# The compile classpath of that slice. Versions track coil's gradle/libs.versions.toml; `annotations`
# arrives transitively via kotlin-stdlib. skiko is needed because `nonAndroidMain` is part of the JVM
# target and imports org.jetbrains.skia.
COORDINATES = [
    "org.jetbrains.kotlin:kotlin-stdlib:2.4.10",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2",
    "com.squareup.okio:okio-jvm:3.17.0",
    "org.jetbrains.kotlinx:atomicfu-jvm:0.33.0",
    "org.jetbrains.skiko:skiko-awt:0.144.6",
]

ENCODING = "UTF-8"

BUILD = """plugins { java }
repositories { mavenCentral() }
dependencies {
%s
}
tasks.register("printCp") {
    val cp = configurations.named("compileClasspath")
    doLast { cp.get().files.sortedBy { it.name }.forEach { println(it.absolutePath) } }
}
"""


def resolve_jars():
    """Resolve COORDINATES into the normal Gradle cache, and return the jar paths."""
    if not shutil.which("gradle"):
        sys.exit("gradle is not on PATH; it is needed to resolve coil's compile classpath")
    tmp = tempfile.mkdtemp(prefix="coil-libs-")
    try:
        deps = "\n".join('    implementation("%s")' % c for c in COORDINATES)
        with open(os.path.join(tmp, "build.gradle.kts"), "w") as f:
            f.write(BUILD % deps)
        with open(os.path.join(tmp, "settings.gradle.kts"), "w") as f:
            f.write('rootProject.name = "coil-libs"\n')
        out = subprocess.run(["gradle", "-q", "--no-daemon", "--console=plain", "printCp"],
                             cwd=tmp, capture_output=True, text=True, check=True).stdout
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    jars = [ln.strip() for ln in out.splitlines()
            if ln.strip().endswith(".jar") and os.path.isfile(ln.strip())]
    if not jars:
        sys.exit("resolved no jars; check network access to Maven Central")
    return sorted(jars, key=os.path.basename)


def main():
    root = os.environ.get("TEST_OSS_ROOT")
    if not root:
        sys.exit("TEST_OSS_ROOT is not set (the Taskfile exports it)")
    corpus = os.path.join(root, "coil")
    if not os.path.isdir(corpus):
        sys.exit("no coil checkout at %s; run `task coil` first" % corpus)

    source_dirs = []
    for s in SOURCE_SETS:
        d = os.path.join(corpus, "coil-core", "src", s, "kotlin")
        if not os.path.isdir(d):
            sys.exit("missing source directory %s; has coil's layout changed?" % d)
        source_dirs.append(d)

    class_path_parts = [{
        "sourceEncoding": ENCODING,
        "name": os.path.basename(j),
        "uri": "file:" + j,
        "library": True,
        "externalLibrary": True,
        "restrictToPackages": [],
    } for j in resolve_jars()]

    config = {
        "workingDirectory": ".",
        "classPathParts": class_path_parts,
        "sourceSets": [{
            "sourceEncoding": ENCODING,
            "name": "coil-core/jvmMain",
            "sourceDirectories": source_dirs,
            "uri": "file://" + os.path.join(corpus, "coil-core", "src") + "/",
            "dependencies": [p["name"] for p in class_path_parts],
        }],
        "alternativeJREDirectory": None,
    }

    target = os.path.join(corpus, "inputConfiguration.json")
    with open(target, "w") as f:
        json.dump(config, f, indent=2)
        f.write("\n")

    kt = sum(len([x for x in files if x.endswith(".kt")])
             for d in source_dirs for _, _, files in os.walk(d))
    print("Wrote %s" % target)
    print("  %d source directories, %d .kt files, %d library jars"
          % (len(source_dirs), kt, len(class_path_parts)))


if __name__ == "__main__":
    main()
