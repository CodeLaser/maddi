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
 * The code-structure graph of a parse: which member needs which (call, reference, type use, hierarchy,
 * javadoc), the analysis order that follows from it, and the primary-type use graph. Moved out of
 * maddi-modification-prepwork on 2026-09-30 (docs/roadmap/split-maddi-into-three-repositories.md, stage 2):
 * it is a property of the parse, not of the modification analysis, and the refactor engine's base tier
 * needs it without the analysis on its class path.
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
    api(project(":maddi-graph"))
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-cst-analysis"))

    // the parsers the tests' CommonTest drives (maddi's own, or javac with -Dmaddi_parser=openJdk)
    testImplementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-java-openjdk"))
    testImplementation("ch.qos.logback:logback-classic")
}

tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4

    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
    systemProperty("maddi_parser", System.getProperty("maddi_parser", "maddi"))
}
