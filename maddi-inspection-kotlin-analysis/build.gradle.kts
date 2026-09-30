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
 * TEST-ONLY host (split stage 3): the maddi-inspection-kotlin tests that run the prep analyzer on Kotlin CST --
 * ports of the Java prep-analyzer tests (Kotlin source in, the same VariableData assertion strings out), the
 * analyzer smoke test and two printer tests. They test that the Kotlin CST feeds the analyzer faithfully, a
 * claim about both tiers; maddi-inspection-kotlin is base and cannot have maddi-mod on its test class path.
 * The test JVM is maddi-inspection-kotlin's: the flat front end (FlatFrontEndTestBootstrap), not the realm.
 */
plugins {
    kotlin("jvm") version "2.4.0"
}

group = "io.codelaser"

dependencies {
    testImplementation(project(":maddi-inspection-kotlin"))
    testImplementation(project(":maddi-inspection-api"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-kotlin-api"))
    testImplementation(project(":maddi-kotlin-k2"))
    testImplementation(project(":maddi-cst-api"))
    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-cst-analysis"))
    testImplementation(project(":maddi-cst-print"))
    testImplementation(project(":maddi-cst-print-kotlin"))
    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-callgraph"))
    testImplementation(project(":maddi-modification-prepwork"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testImplementation("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25) }
}
