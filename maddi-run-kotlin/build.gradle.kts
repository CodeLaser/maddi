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
    application
}
java {
    // 26 (not 25 like most Java modules): this module consumes the Kotlin front-end modules (maddi-inspection-mixed
    // / -kotlin-k2), which the Kotlin plugin compiles to the daemon JDK's bytecode version (26).
    sourceCompatibility = JavaVersion.VERSION_26
    targetCompatibility = JavaVersion.VERSION_26
}
// ⭐ The jars that go INSIDE the realm. Resolvable but not consumable, so nothing here reaches this module's
// own runtimeClasspath -- which is the entire point (G46). The distribution carries them in lib-k2/, beside
// lib/ but never on the launcher's CLASSPATH.
val k2Runtime: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    k2Runtime(project(":maddi-kotlin-k2"))

    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-run-config"))
    // for ParseMixedList: reuse the javac line reader so one log's javac + kotlinc invocations link in one pass
    implementation(project(":maddi-run-openjdk"))

    // the prep-only mixed runner (RunMixedPrepAnalyzer)
    implementation(project(":maddi-inspection-mixed"))      // MixedInspector: shared-core Java+Kotlin parse
    implementation(project(":maddi-kotlin-api"))            // PlaceholderCensus: what the front end could not read
    implementation(project(":maddi-kotlin-realm"))          // K2Realm: the compiler goes in a classloader of its own
    implementation(project(":maddi-modification-prepwork")) // PrepAnalyzer, ComputeAnalysisOrder
    implementation(project(":maddi-modification-analyzer")) // IteratingAnalyzer (--analysis-steps=modification)
    implementation(project(":maddi-modification-link"))    // LinkCodec: the only codec that can write full results
    implementation(project(":maddi-util"))                 // Trie: the shape WriteAnalysisResults takes
    implementation(project(":maddi-modification-common"))   // AnalyzerException (isolated-element reporting)
    implementation(project(":maddi-cst-analysis"))          // PropertyImpl/ValueImpl: read the immutability verdict
    implementation(project(":maddi-graph"))                 // G<Info>
    implementation("com.fasterxml.jackson.core:jackson-databind") // Main reads/writes InputConfiguration JSON
    // ⭐ the command line itself: Main builds it from the Java CLI's own Options object, so the two cannot drift
    implementation("commons-cli:commons-cli")
    implementation(project(":maddi-aapi-parser"))           // AnalysisHintsConfiguration (--preload-…-dirs)

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-inspection-kotlin"))            // TestCoilJvmSlice: the pure-Kotlin path
    // K2RealmTestBootstrap: installs the realm once for the whole module, the way the CLI does in Main
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation(testFixtures(project(":maddi-run-openjdk")))    // TestOssCorpus
}

// the openjdk (javac) front-end that MixedInspector uses reaches into these javac internals
val javacAddExports = listOf(
    "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
)

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(javacAddExports)
    // test-oss corpus location override (see TestOssCorpus): forward -Dtest.oss.root to the forked test
    // JVM, and pass an exported TEST_OSS_ROOT through, so a shell/Taskfile export reaches the worker even
    // via a reused daemon. Unset -> the helper defaults to ../../test-oss. Mirrors maddi-run-openjdk.
    System.getProperty("test.oss.root")?.let { systemProperty("test.oss.root", it) }
    System.getenv("TEST_OSS_ROOT")?.let { environment("TEST_OSS_ROOT", it) }
    // per-type immutability verdicts of a corpus run, for diffing two runs of the same tree (#34); see
    // RunMixedPrepAnalyzer.writeVerdicts. Unset -> nothing is written.
    System.getProperty("maddi.verdictDump")?.let { systemProperty("maddi.verdictDump", it) }
    System.getenv("MADDI_VERDICT_DUMP")?.let { systemProperty("maddi.verdictDump", it) }
    // the placeholder worklist: same forwarding, and for the same reason -- a -D on the Gradle JVM reaches
    // the test fork only if it is named here, and a dump that silently writes nothing looks like a clean run
    System.getProperty("maddi.placeholderDump")?.let { systemProperty("maddi.placeholderDump", it) }
    // ⭐ the tests run against the REALM, exactly as the shipped CLI does: the compiler is never on the test
    // JVM's own classpath, so a corpus run proves the isolation rather than merely coexisting with it
    systemProperty("maddi.k2.classpath", k2Runtime.asPath)
    System.getenv("MADDI_PLACEHOLDER_DUMP")?.let { systemProperty("maddi.placeholderDump", it) }
    jvmArgs("-Xmx" + (System.getenv("TESTXMX") ?: "4G"))
}

// the realm's jars ship beside lib/, not in it: present in the distribution, absent from the CLASSPATH
distributions {
    main {
        contents {
            from(k2Runtime) { into("lib-k2") }
        }
    }
}

application {
    // launcher script `bin/maddi-kotlin`, distribution `maddi-kotlin-<version>.zip` — this bundle is how
    // Kotlin support ships: the K2 'for-ide' jars ride along in lib/ (see PUBLISHING.md)
    applicationName = "maddi-kotlin"
    mainClass = "io.codelaser.maddi.run.kotlinmain.Main"
    // ./gradlew :maddi-run-kotlin:run --args="--compile-log <mixed build log>"
    applicationDefaultJvmArgs = javacAddExports
}
