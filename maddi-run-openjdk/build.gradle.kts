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
    // TestOssCorpus: the test-oss root locator, shared with maddi-run-kotlin's corpus tests
    `java-test-fixtures`
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-modification-analyzer"))
    implementation(project(":maddi-modification-link"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-openjdk"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-java-openjdk"))
    implementation(project(":maddi-java-parser"))
    implementation(project(":maddi-aapi-parser"))

    // to access resource:/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/libs.jar
    runtimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))
    implementation(project(":maddi-run-rewire"))

    implementation("commons-cli:commons-cli")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

application {
    // launcher script `bin/maddi`, distribution `maddi-<version>.zip` (see PUBLISHING.md)
    applicationName = "maddi"
    mainClass = "org.e2immu.analyzer.run.openjdkmain.Main"
    applicationDefaultJvmArgs = listOf(
        "-enableassertions", "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}

run {
    if (project.hasProperty("jvmArgs")) {
        application.applicationDefaultJvmArgs += (project.property("jvmArgs") as String).split("\\s+")
    }
}

tasks.test {
    useJUnitPlatform()
}

// TestEventualRatchet analyses the dogfood input configuration, and that file is GENERATED, under
// dogfood/cst-impl/build/. So a checkout that has not run the generation by hand fails the ratchet on
// its own "does not exist" guard rather than on a verdict -- which is what testrunner reported out of
// ~/git/maddi from 2026-08-11 until this task existed: a setup miss that reads, in a sweep, exactly
// like the regression the ratchet is built to catch.
//
// Committing the file instead is not an option: it holds absolute paths -- the jars, the Gradle cache
// and the SOURCE ROOTS -- so one checkout's copy would silently make another checkout's ratchet
// analyse the first checkout's sources.
//
// GradleBuild rather than Exec("../gradlew"): dogfood is a STANDALONE build (deliberately absent from
// settings.gradle.kts, so nothing there can affect this one), and a nested GradleBuild runs it
// in-process -- no second wrapper, and no second attempt at the whole-box lock this build already
// holds. It declares no outputs, so it always runs; the nested build is itself incremental and costs
// ~2s when there is nothing to do.
val dogfoodInputConfiguration by tasks.registering(GradleBuild::class) {
    group = "verification"
    description = "Generates the dogfood input configuration that TestEventualRatchet analyses."
    // the plugin writes the file; the jars must exist at the project version or dogfood cannot resolve
    // them (dogfood/settings.gradle.kts reads that version out of gradle.properties)
    dependsOn(":maddi-gradleplugin:publishAllPublicationsToLocalPluginRepoRepository",
            ":maddi-support:jar", ":maddi-util:jar")
    dir = file("../dogfood")
    tasks = listOf(":cst-impl:e2immu-write-input-configuration")
    // the plugin's version does not change from one publication to the next, so without this Gradle
    // serves dogfood the cached jar and the generation silently runs the previous plugin (README.md)
    startParameter.isRefreshDependencies = true
}

tasks.named<Test>("slowTest") {
    dependsOn(dogfoodInputConfiguration)
}

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
