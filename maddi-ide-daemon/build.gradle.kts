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

// `java` in a build script resolves to the JavaPluginExtension, so the java.* PACKAGES need importing.
import java.security.MessageDigest

plugins {
    id("java-library-conventions")
    application
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    // analysis pipeline (mirrors maddi-run-main)
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))   // DecoratorImpl, PrepAnalyzer, ComputeCallGraph, ComputeAnalysisOrder
    implementation(project(":maddi-modification-analyzer"))    // IteratingAnalyzerImpl
    implementation(project(":maddi-modification-link"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))             // PropertyImpl keys, ValueImpl
    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-openjdk"))       // JavaInspectorImpl (the integration one is phased out)
    implementation(project(":maddi-inspection-resource"))      // InputConfigurationImpl
    implementation(project(":maddi-java-bytecode"))
    implementation(project(":maddi-aapi-parser"))

    // to access resource:/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/libs.jar
    runtimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))               // ErrorReport, JsonStreaming reuse

    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // the e2immu annotations, supplied to every analyzed project as a classpath part (SourceSetImpl.sourceSetOf)
    implementation(project(":maddi-support"))
}

// The openjdk inspector drives javac internals; these exports are required at runtime (see maddi-run-openjdk).
val openjdkExports = listOf(
    "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
)

application {
    mainClass = "io.codelaser.maddi.ide.daemon.DaemonMain"
    // baked into the generated start script's DEFAULT_JVM_OPTS, so the launched daemon has them
    applicationDefaultJvmArgs = openjdkExports
}

// The analyze test points a real on-disk project at the e2immu annotations (maddi-support, on the test
// classpath) as its "hot class files", exactly as the plugin will point maddi at IntelliJ's output.
tasks.test {
    jvmArgs(openjdkExports)
    useJUnitPlatform()
}

// ---- build stamp ---------------------------------------------------------------------------------
// ⛔ THE IDE RUNS THE DAEMON THE PLUGIN BUNDLES, AND NO VERSION STRING TELLS TWO BUILDS APART.
// 2026-08-24: a plugin whose bundled daemon was built from ANOTHER worktree, at the commit BEFORE a fix
// and with a second, then-uncommitted change on top, announced exactly what a correct build announces --
// "maddi daemon 0.1.0-dev", maddi 0.9.1, "per-module configuration: 64 source set(s)" -- and the defect it
// still carried read as a regression of a fix that had shipped. Settling it took `javap -p` on the installed
// jar. This stamp is that check, printed by the daemon itself at startup and handed to the IDE on handshake.
//
// It is a deterministic function of the SOURCE STATE and never of the clock, so identical sources still
// produce an identical jar (this tree's jars are byte-reproducible; that stays true, and two builds of one
// commit stay indistinguishable *because they are the same build*). Shape:
//     7dbfd36e1                 committed, clean
//     7dbfd36e1+a3f01c9e        the same commit plus uncommitted changes, hashed  <-- the case that fooled us
//     nogit                     no git available (source drop)
// ⚠ It describes the build that produced THIS jar. Every jar in `installDist`/the plugin zip comes from one
// build (installDist is a Sync), so it identifies the whole bundle -- unless someone copies a jar in by hand.
val buildStamp: Provider<String> = providers.provider { gitStamp(rootDir) }
// A single declared output FILE, not a directory: an output directory keeps whatever a previous run left in
// it, so a generator whose path or name changed would go on shipping the old file next to the new one — and a
// stamp that can be stale is the exact failure this whole mechanism exists to make visible.
val buildStampFile = layout.buildDirectory.file("generated/buildStamp/build-stamp.properties")

val generateBuildStamp = tasks.register("generateBuildStamp") {
    description = "Writes the source-state stamp the daemon reports at startup."
    // The stamp itself is the input: when the commit or the working tree changes, the task is out of date.
    // Declared as a Provider so `git` runs only when this task is actually in the graph.
    inputs.property("stamp", buildStamp)
    inputs.property("version", providers.provider { project.version.toString() })
    outputs.file(buildStampFile)
    doLast {
        val f = buildStampFile.get().asFile
        f.parentFile.mkdirs()
        // No Properties.store(): it writes a current-time comment line, which is exactly the kind of
        // clock dependency this stamp must not have.
        f.writeText("source=${buildStamp.get()}\nversion=${project.version}\n")
    }
}

// DaemonMain reads it as a resource NEXT TO ITSELF, so it has to land in that package.
tasks.named<ProcessResources>("processResources") {
    from(generateBuildStamp) {
        into("io/codelaser/maddi/ide/daemon")
    }
}

/** `<short sha>` + `+<8 hex of the working-tree diff>` when dirty; `nogit` when git cannot answer. */
fun gitStamp(dir: File): String {
    val sha = git(dir, "rev-parse", "--short=9", "HEAD") ?: return "nogit"
    // `diff HEAD` covers tracked modifications; `status --porcelain` additionally names untracked files, so
    // a build carrying a NEW file is not reported as clean. Hashed rather than embedded: it must be short.
    val dirt = (git(dir, "diff", "HEAD") ?: "") + (git(dir, "status", "--porcelain") ?: "")
    if (dirt.isBlank()) return sha
    val digest = MessageDigest.getInstance("SHA-256").digest(dirt.toByteArray())
    return sha + "+" + digest.take(4).joinToString("") { "%02x".format(it) }
}

fun git(dir: File, vararg args: String): String? = try {
    val p = ProcessBuilder(listOf("git") + args).directory(dir).redirectErrorStream(false).start()
    val out = p.inputStream.bufferedReader().readText()
    if (p.waitFor() == 0) out.trim() else null
} catch (e: Exception) {
    null
}
