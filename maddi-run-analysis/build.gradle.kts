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

/*
 * The implementation of maddi-analysis-api's AnalysisEngine (split stage 3): the one module of maddi-mod that
 * the other tiers load at run time. The base tier's drivers and the refactor engine never compile against it;
 * the ext tier ships it in runtimeOnly / shade configurations. It also hosts every test that runs the analysis
 * through a driver, because a base module cannot have maddi-mod on even its test class path.
 */
plugins {
    id("java-library-conventions")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    api(project(":maddi-analysis-api"))
    implementation(project(":maddi-callgraph"))
    implementation(project(":maddi-cst-analysis"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-modification-link"))
    implementation(project(":maddi-modification-analyzer"))
    implementation(project(":maddi-aapi-parser"))

    // ---- tests: every driver test that RUNS the analysis (split stage 3). They moved here from maddi-run-openjdk,
    // maddi-run-main and maddi-run-config, which are base and cannot have this module on even their test class
    // path; they keep their packages. The class path below is what they had there, plus this module.
    testImplementation(project(":maddi-run-openjdk"))
    testImplementation(testFixtures(project(":maddi-run-openjdk")))  // TestOssCorpus
    testImplementation(project(":maddi-run-main"))
    testImplementation(project(":maddi-run-config"))
    testImplementation(project(":maddi-run-rewire"))
    testImplementation(project(":maddi-graph"))
    testImplementation(project(":maddi-util"))
    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-cst-io"))
    testImplementation(project(":maddi-cst-print"))
    testImplementation(project(":maddi-inspection-api"))
    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-java-openjdk"))
    testImplementation(project(":maddi-java-parser"))
    testRuntimeOnly(project(":maddi-aapi-archive"))
    testImplementation("commons-cli:commons-cli")
    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
}

// ---- the test JVM of maddi-run-openjdk, verbatim, for the tests that moved from there

// TestEventualRatchet and the dogfood input configuration it analyses moved to maddi-gradleplugin (split stage 5):
// the dogfood build applies that plugin, and the ratchet reaches the analysis through the engine interface.

val javacAddExports = listOf(
    "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
)

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(javacAddExports)
}

tasks.test {
    useJUnitPlatform()
    // -PnoAssertions disables JVM -ea (production-like linking benchmarks)
    enableAssertions = !project.hasProperty("noAssertions")
    System.getProperty("test.oss.root")?.let { systemProperty("test.oss.root", it) }
    System.getenv("TEST_OSS_ROOT")?.let { environment("TEST_OSS_ROOT", it) }
    // 12G: the elasticsearch-server closure under PARALLEL=8; TESTXMX overrides (see maddi-run-openjdk)
    jvmArgs("-Xmx" + (System.getenv("TESTXMX") ?: "12G"))
    jvmArgs(javacAddExports)
    System.getenv("ASPROF")?.let {
        jvmArgs(
            "-agentpath:/opt/homebrew/lib/libasyncProfiler.dylib=$it",
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"
        )
    }
}
