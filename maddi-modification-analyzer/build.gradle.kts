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
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-modification-link"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-cst-io"))
    testImplementation(project(":maddi-cst-print"))
    testImplementation(project(":maddi-inspection-parser"))
    testImplementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-java-bytecode"))
    testImplementation(project(":maddi-java-parser"))

    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-java-openjdk"))

    testImplementation(testFixtures(project(":maddi-modification-common")))

    testRuntimeOnly(project(":maddi-aapi-archive"))
}
tasks.withType<Test> {
    // pass the clone-bench corpus location through to the test JVM, as run-openjdk does for test.oss.root
    System.getProperty("testarchive.root")?.let { systemProperty("testarchive.root", it) }
    System.getenv("TESTARCHIVE_ROOT")?.let { environment("TESTARCHIVE_ROOT", it) }

    maxHeapSize = "2G"
    maxParallelForks = 4

    // forward the TestCloneBench parallelism knob to the forked test JVM (each worker builds its own inspector/
    // runtime; keep it modest so the 2G heap is not exceeded). Default lives in the test itself.
    // TestCloneBench is now @Tag("slow"): the default `test` task skips it, `slowTest` runs it.
    System.getProperty("clonebench.parallelism")?.let { systemProperty("clonebench.parallelism", it) }

    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}
