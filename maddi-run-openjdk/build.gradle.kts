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
    // JavaToKotlinRatchet: the Java -> Kotlin translation ratchet, shared with maddi-mod's run with nullability
    `java-test-fixtures`
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

// TestJavaToKotlinFernflower: the Kotlin printer under test, and the Kotlin compiler that judges its output. The
// compiler runs in a CHILD JVM, so it is resolved into a configuration of its own and handed over as a path: on the
// test class path it would sit beside the Java front end and the analysis, and a compile error would then say as
// much about class-path clashes as about the printer. 2.4.0: the Kotlin of maddi-kotlin-k2's build.
dependencies {
    testFixturesApi(project(":maddi-cst-print-kotlin"))
    testFixturesApi(project(":maddi-cst-print-csharp"))
    testFixturesApi(project(":maddi-inspection-api"))
    testFixturesImplementation(project(":maddi-cst-api"))
    testFixturesImplementation(project(":maddi-cst-impl"))
    testFixturesImplementation(project(":maddi-cst-print"))  // the language-neutral formatter
    testFixturesImplementation(project(":maddi-inspection-openjdk"))
    testFixturesImplementation(project(":maddi-inspection-resource"))
    testFixturesImplementation(project(":maddi-run-config"))
    testFixturesImplementation(testFixtures(project(":maddi-util")))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
    testFixturesImplementation("org.slf4j:slf4j-api")
    testFixturesImplementation("com.fasterxml.jackson.core:jackson-databind")
}
val kotlinCompiler = configurations.create("kotlinCompiler") {
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    kotlinCompiler("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")
}
// withType, not tasks.test: slowTest copies test's jvmArgs and system properties, not its argument providers.
tasks.withType<Test>().configureEach {
    inputs.files(kotlinCompiler).withPropertyName("kotlinCompiler")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dmaddi.test.kotlinCompilerClasspath=" + kotlinCompiler.asPath)
    })
}
// The corpus's own tests run in a child JVM too (JavaToKotlinRatchet's tests stage): JUnit's console launcher and
// the Jupiter engine, at the version of the corpus's junit-jupiter-api (fernflower: 6.0.3).
val junitConsole = configurations.create("junitConsole") {
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    junitConsole("org.junit.platform:junit-platform-console:6.0.3")
    junitConsole("org.junit.jupiter:junit-jupiter-engine:6.0.3")
}
tasks.withType<Test>().configureEach {
    inputs.files(junitConsole).withPropertyName("junitConsole")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dmaddi.test.junitConsoleClasspath=" + junitConsole.asPath)
    })
}

// TestJavaToCSharpFernflower: the judge of the C# printer's output is tools/csharp-check, a .NET tool on Roslyn that
// JavaToCSharpRatchet builds with `dotnet build` (a .NET 10 SDK on the PATH). Its directory, as a system property.
val csharpCheck = rootProject.file("tools/csharp-check")
tasks.withType<Test>().configureEach {
    inputs.dir(csharpCheck).withPropertyName("csharpCheck").optional()
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dmaddi.test.csharpCheck=" + csharpCheck.absolutePath)
    })
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
