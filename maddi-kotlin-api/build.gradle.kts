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

// ⭐ The Kotlin front-end's contract, WITHOUT the Kotlin compiler.
//
// This module exists so the K2 front end can live behind a classloader boundary (see
// docs/kotlin-classloader-isolation.md). Everything here is loaded by the HOST's classloader and
// shared with the realm; everything in maddi-kotlin-k2 is loaded inside it.
//
// ⛔ The one rule: nothing in this module may depend on kotlin-compiler, the '*-for-ide' artifacts,
// or com.intellij. A dependency added here lands on every consumer's runtime classpath, which is
// exactly the defect this boundary exists to fix (G46: a 62 MB fat jar shadowing ANTLR, guava and
// JNA on a consumer's classpath). TestNoCompilerOnTheApiClasspath enforces it.

plugins {
    kotlin("jvm") version "2.4.0"
}

group = "io.codelaser"

dependencies {
    // the shared CST: TypeInfo, Runtime, SourceSet -- the types that CROSS the boundary, so the host
    // and the realm must agree on them (one TypeInfo per FQN is the mixed inspector's core invariant)
    api(project(":maddi-cst-api"))
    api(project(":maddi-inspection-api"))        // CompiledTypesManager, handed to the front end
    api(project(":maddi-inspection-resource"))   // InfoByFqn, the shared registry

    testImplementation(project(":maddi-cst-impl"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

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
