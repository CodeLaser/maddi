#!/usr/bin/env python3
"""
Build a maddi InputConfiguration JSON directly from the Bazel build of this project.

    bazel build //...                                   # ensure jars/actions exist
    bazel aquery 'mnemonic("Javac", kind("java_library", //...))' \
        --output=jsonproto > aquery.json
    python3 bazel_inputconfiguration.py aquery.json input-configuration.json
    maddi --input-configuration input-configuration.json --analysis-steps prep ...

Each java_library becomes a source set (its src/main/java); inter-module dependencies
and external Maven dependencies are supplied as the compiled jars Bazel produced (this is
how maddi resolves across modules -- see how a Maven/Gradle-derived config looks). The
java.se jmod closure is added so the JDK is on the classpath.

Absolute paths (Bazel exec-root, workspace, JDK modules) are embedded, so the output is
specific to this machine/build; regenerate rather than commit it.
"""
import json, os, subprocess, sys

# The java.se module closure (stable JDK set). --input-configuration does not add these
# automatically (only --compile-log does), so they are written into the config.
JAVA_SE = [
    "java.base", "java.compiler", "java.datatransfer", "java.desktop", "java.instrument",
    "java.logging", "java.management", "java.management.rmi", "java.naming", "java.net.http",
    "java.prefs", "java.rmi", "java.scripting", "java.security.jgss", "java.security.sasl",
    "java.sql", "java.sql.rowset", "java.transaction.xa", "java.xml", "java.xml.crypto",
]

def bazel_info(key):
    return subprocess.check_output(["bazel", "info", key], text=True).strip()

def sections(args):
    """JavaBuilder flat args -> {flag: [values]}."""
    d, key = {}, None
    for a in args:
        if a.startswith("--") and a != "--":
            key = a; d.setdefault(key, [])
        elif key is not None:
            d[key].append(a)
    return d

def module_of(binpath):            # bazel-out/.../bin/<module>/lib<module>...jar -> <module>
    return os.path.basename(os.path.dirname(binpath))

def main(aquery_path, out_path):
    execroot = bazel_info("execution_root")
    workspace = bazel_info("workspace")
    absp = lambda execrel: os.path.join(execroot, execrel)

    actions = json.load(open(aquery_path)).get("actions", [])
    modules, lib_jars = {}, {}      # module -> {srcs,jars};  jar name -> file: uri

    for act in actions:
        s = sections(act["arguments"])
        module = module_of(s.get("--output", [None])[0])
        m = modules.setdefault(module, {"srcs": set(), "jars": set()})
        for f in s.get("--sources", []):
            i = f.find("/src/main/java")
            if f.endswith(".java") and i >= 0:
                m["srcs"].add(os.path.join(workspace, f[:i] + "/src/main/java"))
        for j in s.get("--classpath", []):
            base = os.path.basename(j)
            if "rules_jvm_external" in j and (base.startswith("header_") or base.startswith("processed_")):
                # compile against Bazel's real (processed) Maven jar; the part name must
                # equal that jar's filename because maddi keys classpath parts by filename
                name = "processed_" + base.split("_", 1)[1]
                lib_jars[name] = "file:" + absp(os.path.join(os.path.dirname(j), name))
                m["jars"].add(name)
            elif "/bin/" in j and base.startswith("lib") and base.endswith(".jar"):
                dep = module_of(j)
                if dep != module and dep.startswith("maddi-"):
                    name = "lib" + dep + ".jar"           # the sibling module's full compiled jar
                    lib_jars[name] = "file:" + absp(os.path.join(os.path.dirname(j), name))
                    m["jars"].add(name)

    jmods = [{"sourceEncoding": "UTF-8", "name": n, "uri": "jmod:" + n,
              "library": True, "externalLibrary": True, "partOfJdk": True, "restrictToPackages": []}
             for n in JAVA_SE]
    jars = [{"sourceEncoding": "UTF-8", "name": n, "uri": u,
             "library": True, "externalLibrary": True, "restrictToPackages": []}
            for n, u in sorted(lib_jars.items())]
    source_sets = [{
        "sourceEncoding": "UTF-8",
        "name": mod + "/main",
        "sourceDirectories": sorted(modules[mod]["srcs"]),
        "uri": "file:" + os.path.join(execroot, "bazel-out/bin", mod),
        "restrictToPackages": [],
        "dependencies": sorted(modules[mod]["jars"]),
    } for mod in sorted(modules)]

    json.dump({"workingDirectory": ".", "classPathParts": jmods + jars,
               "sourceSets": source_sets, "alternativeJREDirectory": None},
              open(out_path, "w"), indent=2)
    print(f"{len(source_sets)} source sets, {len(jars)} library jars, {len(jmods)} jmods -> {out_path}")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
