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

// ⭐ The realm: where the Kotlin compiler and the 62 MB of third-party libraries it bundles are loaded,
// so they are NOT on anybody's flat classpath. See docs/kotlin-classloader-isolation.md (G46).

plugins {
    id("java-library-conventions")
    // ⚠ for the TESTS, which are Kotlin (they assert on CST identity across the boundary). Without this the
    // .kt sources are silently not compiled and the suite is green because it is empty — which is how the
    // first version of this module "passed" in 659 ms.
    kotlin("jvm") version "2.4.0"
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25) }
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// The jars that go INSIDE the realm: maddi-kotlin-k2 and everything it needs at run time. ⚠ Resolvable but
// NOT part of this module's own runtime classpath -- that is the whole point. A consumer declares the same
// configuration and hands its files to K2Realm; nothing lands on their runtimeClasspath.
val k2Runtime: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    api(project(":maddi-kotlin-api"))
    implementation("org.codehaus.plexus:plexus-classworlds:2.9.0")

    k2Runtime(project(":maddi-kotlin-k2"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // ⭐ the same mechanism a consumer uses: resolve the K2 runtime into a path and hand it over. The test
    // JVM's OWN classpath stays free of the compiler, which is what makes the isolation testable at all.
    systemProperty("maddi.k2.classpath", k2Runtime.asPath)
}
