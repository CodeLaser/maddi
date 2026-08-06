
## Running maddi on these projects

Prereqs: JDK 26 (`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-26.jdk/Contents/Home`).
The openjdk front-end needs the javac `--add-exports`; keep them in `MADDI_EXPORTS`:

```
MADDI_EXPORTS="--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
 --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
 --add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
 --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
 --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
```

Analyse **prep only** for now (`-DanalysisSteps=prep` / `--analysis-steps prep`); modification is still too slow.

### Maven projects (e.g. Jenkins, Guava) — the maddi Maven plugin

Install the plugin once (in the maddi repo): `./gradlew :maddi-mvnplugin:publishToMavenLocal`.

The plugin has two goals of interest, both driven off the same computed `InputConfiguration`:

- **`write-input-configuration`** — write `target/inputConfiguration.json` and exit (no analysis). Use this to
  *generate an input configuration* for later runs, for diffing, or for handing to another tool.
- **`run`** — compute the config *and* run the analyzer (`-DanalysisSteps=prep`).

#### Generate an input configuration (`write-input-configuration`)

From anywhere in the reactor, select the target module with `-pl` and bind `generate-test-sources` first (so
every generated/added source root is registered):

```
# Jenkins (module cli) — plain src/main/java, src/test/java layout
cd jenkins
env MAVEN_OPTS="$MADDI_EXPORTS -Xmx6G" mvn \
  -pl cli generate-test-sources io.codelaser:maddi-mvnplugin:0.8.2:write-input-configuration
# -> jenkins/cli/target/inputConfiguration.json  (2 source sets, 22 classpath parts)

# Guava (module guava) — non-standard <sourceDirectory>src</sourceDirectory>
cd guava
mvn -pl guava -am install -DskipTests            # once: publish the reactor jars to ~/.m2
env MAVEN_OPTS="$MADDI_EXPORTS -Xmx6G" mvn \
  -pl guava generate-test-sources io.codelaser:maddi-mvnplugin:0.8.2:write-input-configuration
# -> guava/guava/target/inputConfiguration.json  (main SS points at guava/src, test at guava/test)
```

The plugin reads Maven's own `compileSourceRoots`/`testCompileSourceRoots`, so a custom `<sourceDirectory>`
(Guava's `src`) or `build-helper` `add-test-source` roots are picked up automatically — no special handling.

Then run the analyzer straight off the generated JSON (validates the config end-to-end):

```
cd ~/git/maddi
./gradlew :maddi-run-openjdk:run --args="\
  --input-configuration /Users/bnaudts/git/test-oss/jenkins/cli/target/inputConfiguration.json \
  --analysis-steps prep"
```

#### Or compute + analyse in one step (`run`)

```
cd jenkins
env MAVEN_OPTS="$MADDI_EXPORTS -Xmx6G" mvn \
  -pl cli generate-test-sources io.codelaser:maddi-mvnplugin:0.8.2:run -DanalysisSteps=prep
```

Notes: run `generate-test-sources` first (maddi analyses main *and* test) so every generated/added source root
is registered — this includes `build-helper-maven-plugin` `add-test-source` roots bound to that phase (e.g.
langchain4j-core pulls in `../langchain4j-test/src/main/java` there; plain `generate-sources` misses it);
`jmods` defaults to `java.se` (the whole JDK on the classpath) — override with `-Djmods=java.base` for a leaner run;
source directories are emitted absolute, so the goal no longer needs to run from the module directory;
**for any multi-module reactor (Guava, Timefold, langchain4j, …) run `mvn install -pl <module> -am -DskipTests`
first** so the plugin can resolve sibling reactor modules as jars from `~/.m2` — this is the usual reason
`write-input-configuration` "fails" on a fresh checkout (`Cannot resolve … :jar`).

Both Jenkins (`cli`) and Guava (`guava`) verified generating + prep-analysing green on 2026-07-17
against the merged `kotlin`/`main` plugin (0.8.2).

### Gradle projects (e.g. Fernflower) — derive a config from the compile log

No Maven plugin; capture the Gradle compiler arguments and feed them to maddi's `--compile-log`:

```
cd fernflower
./gradlew --no-build-cache --rerun-tasks compileJava compileTestJava --debug 2>&1 \
  | grep -a 'Compiler arguments:' > /tmp/compile.log
env JAVA_TOOL_OPTIONS="$MADDI_EXPORTS -Xmx6G" maddi \
  --compile-log /tmp/compile.log --write-input-configuration inputConfiguration.json
env JAVA_TOOL_OPTIONS="$MADDI_EXPORTS -Xmx6G" maddi \
  --input-configuration inputConfiguration.json --analysis-steps prep
```

(`maddi` = the openjdk runner, `org.e2immu.analyzer.run.openjdkmain.Main`; the `--compile-log` path adds the
`java.se` jmod closure automatically. See `maddi-run-openjdk/running-examples.md` in the maddi repo.)

## Cycle structure of the wired corpus

Measured 2026-07-21 on the 8 wired projects, at the module scope each `config:*` task pins. "Primary
types" is what maddi parses; "largest SCC" is the biggest strongly connected component of the
primary-type graph (`SccCondensation`, not `Linearize`'s cluster, which overstates it).

Only the type graph is needed for this, so it comes from **prep alone** — no modification analysis,
seconds per project rather than minutes.

| project | scope | primary types | largest SCC | share |
|---|---|---|---|---|
| fernflower | whole (Gradle) | 227 | **164** | **72%** |
| camel | `core/camel-core-model` | 399 | 158 | 40% |
| activemq | `activemq-broker` | 500 | 176 | 35% |
| langchain4j | `langchain4j-core` | 518 | 7 | 1% |
| guava | `guava` | 610 | 118 | 19% |
| jenkins | `core` | 1475 | **639** | **43%** |
| timefold-solver | whole reactor (65 source sets) | 3574 | 842 | 24% |
| elasticsearch | whole (27 source sets) | 8467 | **3680** | **43%** |

Practical consequences when picking a corpus for cycle work:

- **fernflower is the best small proxy for a tangled monorepo** — 72% of its types in one component,
  and it parses in about 4 seconds. Far better for iteration than timefold.
- **elasticsearch is the most realistic at scale**: 3680 types in one component; jenkins is the
  next best at 639 of 1475 (43%).
- **guava does have cycles** — 118 types over 21 components. Worth stating because the opposite was
  assumed here for a while; it was simply never loadable to check.
- **langchain4j has no cycle worth studying** (largest SCC 7, spread over 28 tiny components). It is
  a good corpus for *modification/immutability* work and a poor one for cycles.

guava and jenkins used to fail to load entirely, on `codelaser-metrics-cycle`'s
`GraphComputerImpl` assert "From anonymous inner to enclosing: should be marked as recursion". The
assert was wrong — an anonymous subclass that *overrides* the method it is declared in (Guava's
`Joiner.useForNull` is the canonical case) produces that edge shape legitimately, as an override
rather than a call. Fixed 2026-07-21; both projects load.

## Status: Built

Note: each line with 4 number is the output of `cloc` for that language.

### ActiveMQ

```
mvn clean install -DskipTests
```

Java                          4697         127230         242504         588820


### Camel

```
./mvnw install -DskipTests
```

675 modules!

Java                         26710         474618        1221995        2539205

### Gradle

```
./gradlew compileTestJava
```

Groovy                             6566         181162         116505         854036
Java                              11416         125948         285637         581293
Kotlin                             5710          84679         212282         356044

### ElasticSearch

```
./gradlew compileTestJava
```

Java                         32084         760564         757554        5118018

### Fernflower

```
./gradlew compileTestJava
```

Java                           501          10396           3092          57685

### Google Guava

```
mvn install -DskipTests
```

Java                          1926          60575         133092         350565

### Hadoop

```
./mvnw install -DskipTests
```
takes ages, especially the shaded builds.

```
./mvnw test-compile
```

Java                         12618         403631         830231        2809199


### Hazelcast

```
./mvnw clean install -DskipTests
```

Java                         11576         287855         362382        1495977


### Hibernate ORM

```
./gradlew compileTestJava
```

Java                           23401         422303         533589        1846364

### Hive

```
mvn install -DskipTests
```
succeeds on almost all projects (54/57)

Java                          9532         387854         486579        2454554

### Jenkins

```
mvn install -DskipTests -Dlicense.disableCheck
```

Java                          2028          44345         111027         217109

### Keycloak

```
./mvnw clean install -DskipTests
```

Java                          8299         195924         200218         794721

### Langchain4j

```
mvn clean install -DskipTests
```

Java                          2864          59420          42168         272317

### Pulsar

```
./gradlew compileTestJava -PskipJavaVersionCheck
```

Java                          4755         117863         189611         803604


### Quarkus

```
./mvnw clean install -DskipTests
```

Java                           22298         299007         206293        1427748


### Timefold-solver

```
./mvnw  clean  install -DskipTests
```

Java                            3620          53062          34337         257969


### Tomcat

```
ant
```

Java                                   2807          95637         210854         377480


### Trino

use the Temurin build, in bash:
```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-26.jdk/Contents/Home ./mvnw clean install -DskipTests=true -Duse.jdk17=true
```

Java                         11182         234865         223771        1605465

### Wildfly

Add commons-collections license
```
<dependency>
    <groupId>commons-collections</groupId>
    <artifactId>commons-collections</artifactId>
    <licenses>
        <license>
            <name>Apache License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>
</dependency>
```
to the license files
```
find . -name "*licenses.xml"
```
then
```
./mvnw clean install -DskipTests
```

Java                                  10094         128067         144293         604068


## Status: Not yet built/Cannot parse/Wrong JDK/...


### Cassandra

Ant build system


### Druid

???


JavaScript frontend



### Neo4J

ignore, contains a lot of Scala


### Spring

??


### Netty

Needs 'cmake', install via brew.
```
brew install autoconf automake libtool
./mvnw clean install -DskipTests
```
still, fails on cmake?

### Sonarqube

```
./gradlew -Dorg.gradle.java.home=$(/usr/libexec/java_home -v 21) --stop
./gradlew -Dorg.gradle.java.home=$(/usr/libexec/java_home -v 21) compileTestJava
```


