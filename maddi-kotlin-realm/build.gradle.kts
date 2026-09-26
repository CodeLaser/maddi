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
// so they are NOT on anybody's flat classpath. See docs/design/kotlin-classloader-isolation.md (G46).

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
    // ⛔ An INPUT, not a string. `systemProperty(…, k2Runtime.asPath)` handed the realm the jars' PATHS and told
    // Gradle nothing: no task dependency on building them, no re-run when they change. A working copy that had
    // built maddi-kotlin-k2 for other reasons passed; this one handed the realm a jar from BEFORE the realm
    // existed (no K2FrontEnd, no service file) and 4 of K2RealmTest's 5 failed with "no KotlinFrontEnd
    // implementation is visible"; a clean checkout hands it a path that does not exist.
    inputs.files(k2Runtime).withPropertyName("k2Runtime").withNormalizer(ClasspathNormalizer::class)
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-Dmaddi.k2.classpath=" + k2Runtime.asPath) })
}
