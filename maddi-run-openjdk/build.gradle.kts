/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    id("java-library-conventions")
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    implementation(project(":maddi-callgraph"))  // ComputeCallGraph & co., moved out of prepwork (split stage 2)
    implementation(project(":maddi-cst-api"))
    testImplementation(project(":maddi-support"))
    implementation(project(":maddi-analysis-api"))  // the modification analysis, as a service (split stage 3)
    // Corpora, the corpus locator (it moved here from this module's own test fixtures)
    testImplementation(testFixtures(project(":maddi-util")))

    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-inspection-openjdk"))
    implementation(project(":maddi-inspection-resource"))

    // to access resource:/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/libs.jar
    runtimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))
    implementation(project(":maddi-run-rewire"))

    implementation("commons-cli:commons-cli")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

// The `maddi` launcher and distribution moved to maddi-cli (mod): this driver finds the modification analysis as
// a service, and a base module cannot carry it (split stage 3).

tasks.test {
    useJUnitPlatform()
}

// The dogfood input-configuration task and the eventual ratchet that reads it moved to maddi-run-analysis with
// every other test that runs the analysis (split stage 3).

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        )
    )
}

tasks.test {
    useJUnitPlatform()
    // -PnoAssertions disables JVM -ea; the linking engine's debug sanity assertions (consistencyCheck,
    // checkDuplicateNames) are not production behaviour, so turn them off to benchmark production-like linking.
    enableAssertions = !project.hasProperty("noAssertions")
    // test-oss corpus location override (see TestOssCorpus): forward -Dtest.oss.root to the forked
    // test JVM, and pass an exported TEST_OSS_ROOT through, so a shell/Taskfile export reaches the
    // worker even via a reused daemon. Unset -> the helper defaults to ../../test-oss.
    System.getProperty("test.oss.root")?.let { systemProperty("test.oss.root", it) }
    System.getenv("TEST_OSS_ROOT")?.let { environment("TEST_OSS_ROOT", it) }
    jvmArgs(
        // 6G showed heavy GC under PARALLEL=8 (8 threads allocating link graphs concurrently);
        // 8G was marginal for the elasticsearch-server closure: one green run pinned at the ceiling,
        // one executor died of heap space at teardown, one rerun GC-thrashed (2026-08-06). 12G runs
        // it with real headroom; TESTXMX still overrides in either direction.
        "-Xmx" + (System.getenv("TESTXMX") ?: "12G"),
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
    // ASPROF=<agent options> attaches async-profiler to the test JVM, e.g.
    //   ASPROF=start,event=cpu,file=/tmp/profile.collapsed  (format inferred from the extension)
    // pair with -PnoAssertions for production-like profiles
    System.getenv("ASPROF")?.let {
        jvmArgs(
            "-agentpath:/opt/homebrew/lib/libasyncProfiler.dylib=$it",
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"
        )
    }
}
