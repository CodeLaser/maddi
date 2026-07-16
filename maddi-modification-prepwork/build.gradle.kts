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
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))

    /*
    Parsing is a test-only concern here: prepwork's main analyses a CST, it does not build one. Everything below
    used to be an 'implementation', which put it on the runtime class path of every prepwork consumer -- so
    maddi-run-openjdk dragged in the in-house parser (and the congocc parser, and the bytecode reader) that it has
    no use for. main referenced none of them: module-info only ever required inspection.integration, and no source
    file used it, while the other four were not in module-info at all and so could not be referenced by main even
    in principle -- main is compiled in module mode, where 'requires' is the whole of the visible world.
     */
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

    val impl = System.getProperty("maddi_parser", "maddi")

    // Pass it forward down to the worker JVM execution context
    systemProperty("maddi_parser", impl)

    // Visual logging to your terminal so you always know which version is active
    logger.lifecycle("Project [${project.name}] executing test suite targeting: $impl")
}
