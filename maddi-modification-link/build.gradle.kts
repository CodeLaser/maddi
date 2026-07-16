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


plugins {
    id("java-library-conventions")
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    api(project(":maddi-cst-api"))
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-impl"))

    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-inspection-parser"))

    testRuntimeOnly(project(":maddi-aapi-archive"))
    testImplementation(project(":maddi-cst-print"))
    testImplementation(project(":maddi-inspection-parser"))
    testImplementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-java-bytecode"))
    testImplementation(project(":maddi-java-parser"))
    testImplementation(project(":maddi-aapi-parser"))

    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-java-openjdk"))

    testImplementation(testFixtures(project(":maddi-modification-common")))
}

tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = (findProperty("testForks") as String?)?.toInt() ?: 4
    // forkEvery=0 (gradle default): one JVM per fork runs all its classes. The test suite is deterministic in this
    // mode — verified by three identical parallel runs plus serial==monolith==isolated (0 flips). An earlier
    // forkEvery=1 (fresh JVM per class) was added on the belief the suite was order-unstable; that was a
    // measurement artifact (inconsistent HTML-entity decoding when diffing two runs), and forkEvery=1 only cost
    // ~20% wall time (261s vs 216s). Keep it configurable in case the known intermittent javac SharedNameTable
    // issue (see -XDuseUnsharedTable below) ever needs a per-class reset: -PforkEvery=1.
    forkEvery = (findProperty("forkEvery") as String?)?.toLong() ?: 0L

    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}
