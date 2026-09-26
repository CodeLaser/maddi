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
    // ⭐ The front end's CONTRACT, which the host loads and this module implements. Everything the host
    // is allowed to see lives there; see docs/design/kotlin-classloader-isolation.md and the ⛔ rule in its build.
    api(project(":maddi-kotlin-api"))
    // The shared CST this front-end produces (Runtime factories, TypeInfo, ParameterizedType, ...).
    api(project(":maddi-cst-api"))
    // The shared type registry (InfoByFqn) used across language front-ends.
    implementation(project(":maddi-inspection-resource"))
    // the front end reports what it could not do faithfully (elvis re-evaluations); the realm shares
    // org.slf4j with the host, so those lines come out of the host's appenders
    implementation("org.slf4j:slf4j-api:2.0.16")

    // The compiler itself (PSI + FIR internals the Analysis API sits on top of). Maven Central.
    // ⛔ Minus its UPSTREAM coroutines: the Analysis API needs IntelliJ's patched copy (runtimeOnly below), and
    // an EXCLUDE is the only form of that rule a consumer inherits. The dependencySubstitution further down
    // governs this project's own configurations and nothing else -- the realm's k2Runtime, maddi-run-kotlin's
    // lib-k2 and every consumer's received BOTH jars, upstream 1.8.0 first, so the realm ran upstream coroutines
    // with one patched class beside it (RealmCoroutinesTest).
    implementation("org.jetbrains.kotlin:kotlin-compiler:$analysisApiVersion") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }

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
    // The Analysis API needs IntelliJ's *patched* coroutines, which add
    // kotlinx.coroutines.internal.intellij.IntellijCoroutines (absent from upstream). The standard
    // coroutines kotlin-compiler drags in is substituted out below so only this one is present.
    // (cf. detekt#9176; remove if KT-81457 ever folds this back upstream.)
    runtimeOnly("org.jetbrains.intellij.deps.kotlinx:kotlinx-coroutines-core:1.10.2-intellij-1")

    // Test-only: a concrete Runtime, and SourceSet construction.
    testImplementation(project(":maddi-cst-impl"))
    // NonLocalReturnTest prints the CST back as Kotlin: a `return@forEach` must survive the round trip
    testImplementation(project(":maddi-cst-print-kotlin"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        val intellijCoroutines = module("org.jetbrains.intellij.deps.kotlinx:kotlinx-coroutines-core:1.10.2-intellij-1")
        substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-core")).using(intellijCoroutines)
        substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")).using(intellijCoroutines)
    }
}

// No jvmToolchain(): match the rest of the project, which compiles on the Gradle daemon JDK
// (JDK 26 here) rather than a provisioned toolchain.
// JVM 25 bytecode, as every other maddi module (their java blocks) and the jfocus convention: left to the daemon JDK
// (26), these three alone came out as class-file version 70, which a 25 consumer can neither compile against nor load.
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25) }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
