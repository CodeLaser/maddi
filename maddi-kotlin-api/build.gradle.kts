/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ⭐ The Kotlin front-end's contract, WITHOUT the Kotlin compiler.
//
// This module exists so the K2 front end can live behind a classloader boundary (see
// docs/design/kotlin-classloader-isolation.md). Everything here is loaded by the HOST's classloader and
// shared with the realm; everything in maddi-kotlin-k2 is loaded inside it.
//
// ⛔ The one rule: nothing in this module may depend on kotlin-compiler, the '*-for-ide' artifacts,
// or com.intellij. A dependency added here lands on every consumer's runtime classpath, which is
// exactly the defect this boundary exists to fix (G46: a 62 MB fat jar shadowing ANTLR, guava and
// JNA on a consumer's classpath). TestNoCompilerOnTheApiClasspath enforces it.

plugins {
    kotlin("jvm") version "2.4.0"
    // a Maven publication, as every base library has (maddi build-logic; split stage 6)
    id("maddi-publishing")
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

// No module-info (a Kotlin module), but the IDE daemon is a named module that requires this one: an
// automatic module name is what puts the jar on its compile module path rather than the unnamed classpath.
tasks.jar {
    manifest { attributes("Automatic-Module-Name" to "io.codelaser.maddi.kotlin.api") }
}
