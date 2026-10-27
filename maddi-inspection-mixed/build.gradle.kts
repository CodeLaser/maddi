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

// The mixed-language driver: parses a mixed Java+Kotlin module over one shared core (openjdk + K2 front-ends
// sharing one Runtime / InfoByFqn / CompiledTypesManager). Depends on BOTH front-ends, so it lives in its own
// module rather than polluting either. See mixed-language-integration.md (in maddi-inspection-kotlin).

plugins {
    kotlin("jvm") version "2.4.0"
    // a Maven publication, as every base library has (maddi build-logic; split stage 6)
    id("maddi-publishing")
}

group = "io.codelaser"

dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-cst-api"))
    implementation(project(":maddi-inspection-resource"))
    // ⭐ the CONTRACT, and ONLY the contract. A reference to a K2 type from this module's main code is a
    // compile error, and the implementation is not a runtime dependency either -- that `runtimeOnly` is
    // precisely how 62 MB of compiler reached a consumer's classpath (G46). The host installs a front end
    // (KotlinFrontEnds.install), normally one loaded in a realm; see maddi-kotlin-realm.
    implementation(project(":maddi-kotlin-api"))
    // ⚠ tests MAY name the implementation (they exercise it); main code may not — that asymmetry is the
    // boundary, and it is enforced by these two lines rather than by anyone remembering it.
    testImplementation(project(":maddi-kotlin-k2"))
    implementation(project(":maddi-inspection-kotlin"))  // JavaStubGenerator
    implementation(project(":maddi-inspection-openjdk")) // the openjdk (javac) Java front-end
    implementation(project(":maddi-java-openjdk"))       // SourceSetInterleave

    testImplementation(project(":maddi-cst-impl"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    // FlatFrontEndTestBootstrap: the explicit install of the flat front end these tests use
    testImplementation("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // the openjdk (javac) Java front-end reaches into javac internals, so the test JVM (and any runtime that
    // uses MixedInspector) needs the same --add-exports as the maddi-inspection-openjdk module.
    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}

// JVM 25 bytecode, as every other maddi module (their java blocks) and the jfocus convention: left to the daemon JDK
// (26), these three alone came out as class-file version 70, which a 25 consumer can neither compile against nor load.
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25) }
}

// No module-info (a Kotlin module), but the IDE daemon is a named module that requires this one: an
// automatic module name is what puts the jar on its compile module path rather than the unnamed classpath.
tasks.jar {
    manifest { attributes("Automatic-Module-Name" to "io.codelaser.maddi.inspection.mixed") }
}
