
#!/usr/bin/env bash
#
# mvn-javac-debug.sh — inspect the `-X` (debug) output of a Maven build to see, per reactor
# module, the javac compilation that ran: the output directory (-d) and the classpath,
# or *why* no javac ran (module skipped because it was up to date, or had no sources).
#
# This is the input maddi uses to reconstruct a project's classpath. The most common
# surprise is a module that shows up as "UP-TO-DATE": its target/classes already existed,
# so the maven-compiler-plugin skipped it and emitted no javac command. Run the build with
# `clean` to force every module to recompile.
#
# Capture the build output with, e.g.:
#     ./mvnw clean test-compile -X -Dmaven.build.cache.enabled=false -Dstyle.color=never > javac.txt 2>&1
#   (run single-threaded; do NOT use -T, which interleaves modules and breaks the pairing)
#
# Usage:
#     scripts/javac-debug.sh <mvn-X-output.txt>             # summary: one line per module
#     scripts/javac-debug.sh <mvn-X-output.txt> <filter>    # full classpath for modules whose
#                                                           # coordinates/name contain <filter>
#
# Examples:
#     scripts/javac-debug.sh javac.txt                      # overview of all modules
#     scripts/javac-debug.sh javac.txt core                 # full classpath of the 'core' module(s)
#
set -euo pipefail

file=${1:?usage: $0 <mvn-X-output.txt> [module-filter]}
filter=${2:-}

awk -v filter="$filter" '
  # Reactor banner:  [INFO] --------< groupId:artifactId >--------   (precedes "Building")
  /\[INFO\] -+< .* >-+/ { c = $0; sub(/.*< */, "", c); sub(/ *>.*/, "", c); pend = c; next }

  # Module start:    [INFO] Building <name> <version> [n/total]
  /^\[INFO\] Building / {
      cur = ++n
      nm = $0; sub(/^\[INFO\] Building /, "", nm)
      name[cur] = nm; coords[cur] = pend; pend = ""; next
  }

  # Compiler-plugin skips (no javac command is emitted in these cases)
  /^\[INFO\] No sources to compile/  { if (cur && note[cur] !~ /no-sources/)  note[cur] = note[cur] " [no-sources]";  next }
  /^\[INFO\] Nothing to compile/     { if (cur && note[cur] !~ /UP-TO-DATE/)  note[cur] = note[cur] " [UP-TO-DATE]";  next }

  # The javac command line is the single [DEBUG] line right after "Command line options:"
  /^\[DEBUG\] Command line options:/ { expect = 1; next }
  expect {
      expect = 0
      if (cur == 0) next
      line = $0; sub(/^\[DEBUG\] /, "", line)
      nf = split(line, a, " ")
      dval = ""; cp = ""; mp = ""
      for (i = 1; i <= nf; i++) {
          if (a[i] == "-d")                                                          dval = a[i + 1]
          else if (a[i] == "-classpath" || a[i] == "-cp" || a[i] == "--class-path")  cp   = a[i + 1]
          else if (a[i] == "-p" || a[i] == "--module-path")                          mp   = a[i + 1]
      }
      if (dval != "") {
          key = cur SUBSEP dval                 # de-duplicate: the plugin logs the command twice
          if (!(key in seen)) {
              seen[key] = 1
              j = ++cnt[cur]
              D[cur, j] = dval; C[cur, j] = cp; MP[cur, j] = mp
          }
      }
      next
  }

  END {
      for (k = 1; k <= n; k++) {
          if (filter != "" && index(coords[k], filter) == 0 && index(name[k], filter) == 0) continue
          printf("== %s%s\n", (coords[k] != "" ? coords[k] : name[k]), note[k])
          if (coords[k] != "") printf("   (%s)\n", name[k])
          for (j = 1; j <= cnt[k]; j++) {
              kind = (D[k, j] ~ /test-classes/) ? "test" : "main"
              printf("   javac [%s] -d %s\n", kind, D[k, j])
              m = split(C[k, j], jars, ":")
              if (filter != "") { for (x = 1; x <= m; x++) printf("        %s\n", jars[x]) }
              else                printf("        classpath: %d entries\n", m)
              # entries on the module path are the ones javac actually treats as modules
              if (MP[k, j] != "") {
                  mm = split(MP[k, j], mods, ":")
                  if (filter != "") { for (x = 1; x <= mm; x++) printf("    [mod] %s\n", mods[x]) }
                  else                printf("        module-path: %d entries (treated as modules)\n", mm)
              }
          }
          if (cnt[k] == 0 && note[k] == "") printf("   (no javac command captured)\n")
      }
  }
' "$file"
