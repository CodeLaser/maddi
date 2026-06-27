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

// M0 spike: prove the Kotlin K2 Standalone Analysis API resolves and runs.
// Deliberately self-contained (no java-library-conventions/platform BOM) to keep the
// IntelliJ-dependencies graph isolated from the rest of the build while we de-risk it.

plugins {
    kotlin("jvm") version "2.4.0"
}

group = "io.codelaser"

// The Analysis API artifacts are versioned in lockstep with the Kotlin compiler.
val analysisApiVersion = "2.4.0"

dependencies {
    // The compiler itself (PSI + FIR internals the Analysis API sits on top of). Maven Central.
    implementation("org.jetbrains.kotlin:kotlin-compiler:$analysisApiVersion")

    // K2 Analysis API '*-for-ide' artifacts (intellij-dependencies repo). Names verified for 2.4.0.
    // Transitives are declared via shaded *-base artifacts that are NOT separately published, so we
    // list the set explicitly and disable transitivity (the canonical standalone recipe).
    val forIde = listOf(
        "analysis-api-for-ide",
        "analysis-api-k2-for-ide",            // FIR implementation (was high-level-api-fir-for-ide)
        "analysis-api-impl-base-for-ide",
        "low-level-api-fir-for-ide",
        "analysis-api-platform-interface-for-ide",
        "symbol-light-classes-for-ide",
        "analysis-api-standalone-for-ide",
    )
    forIde.forEach { a ->
        implementation("org.jetbrains.kotlin:$a:$analysisApiVersion") { isTransitive = false }
    }

    // Runtime deps the standalone session needs but the stripped '*-for-ide' artifacts don't carry.
    // Discovered empirically in M0 by following NoClassDefFoundError chains.
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    runtimeOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    // IntelliJ's coroutines carry kotlinx.coroutines.internal.intellij.*, absent from the 1.8.0
    // that kotlin-compiler pulls. Force a version that includes it.
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
    }
}

// No jvmToolchain(): match the rest of the project, which compiles on the Gradle daemon JDK
// (JDK 26 here) rather than a provisioned toolchain. Kotlin 2.4 caps its target at JVM 25.

tasks.withType<Test> {
    useJUnitPlatform()
}
