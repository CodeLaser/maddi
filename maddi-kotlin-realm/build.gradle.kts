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

// No module-info (a Kotlin module), but the IDE daemon is a named module that requires this one: an
// automatic module name is what puts the jar on its compile module path rather than the unnamed classpath.
tasks.jar {
    manifest { attributes("Automatic-Module-Name" to "io.codelaser.maddi.kotlin.realm") }
}
