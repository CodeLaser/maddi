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

// The mixed-language driver: parses a mixed Java+Kotlin module over one shared core (openjdk + K2 front-ends
// sharing one Runtime / InfoByFqn / CompiledTypesManager). Depends on BOTH front-ends, so it lives in its own
// module rather than polluting either. See mixed-language-integration.md (in maddi-inspection-kotlin).

plugins {
    kotlin("jvm") version "2.4.0"
}

group = "io.codelaser"

dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-cst-api"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-kotlin-k2"))          // KotlinScan
    implementation(project(":maddi-inspection-kotlin"))  // JavaStubGenerator
    implementation(project(":maddi-inspection-openjdk")) // the openjdk (javac) Java front-end

    testImplementation(project(":maddi-cst-impl"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
