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
    testImplementation(project(":maddi-support"))
    api(project(":maddi-graph"))
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-cst-analysis"))

    // the parsers the tests' CommonTest drives (maddi's own, or javac with -Dmaddi_parser=openJdk)
    testImplementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-inspection-openjdk"))
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
