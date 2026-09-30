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
 * The `maddi-kotlin` command-line distribution (split stage 3): the mixed Java+Kotlin driver (maddi-run-kotlin,
 * base) together with the modification analysis it finds as a service (maddi-run-analysis, mod), and the K2
 * front end's jars in lib-k2/. Moved here from maddi-run-kotlin, which is base and cannot carry the engine.
 *
 *   ./gradlew :maddi-cli-kotlin:run --args="--compile-log <mixed build log>"
 *   ./gradlew :maddi-cli-kotlin:distZip   → maddi-kotlin-<version>.zip
 */
plugins {
    id("java-library-conventions")
    application
}
java {
    // 26, like maddi-run-kotlin: the Kotlin front-end modules are compiled to the daemon JDK's bytecode version
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
    runtimeOnly(project(":maddi-run-kotlin"))    // the driver
    runtimeOnly(project(":maddi-run-analysis"))  // the AnalysisEngine the driver loads
}

// the openjdk (javac) front-end that MixedInspector uses reaches into these javac internals
val javacAddExports = listOf(
    "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
)

// `run` goes through the realm as the tests and the shipped CLI do. Without this, `run` on any input with Kotlin
// sources died in K2Realm.discover ("cannot find the Kotlin front end's jars") -- found when the corpus
// catalogue's parse phase first ran a Kotlin entry (coil, 2026-09-26).
tasks.named<JavaExec>("run") {
    inputs.files(k2Runtime).withPropertyName("k2Runtime").withNormalizer(ClasspathNormalizer::class)
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-Dmaddi.k2.classpath=" + k2Runtime.asPath) })
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
    // Kotlin support ships: the K2 'for-ide' jars ride along in lib-k2/ (see PUBLISHING.md)
    applicationName = "maddi-kotlin"
    mainClass = "io.codelaser.maddi.run.kotlinmain.Main"
    applicationDefaultJvmArgs = javacAddExports
}
