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
 * The `maddi` command-line distribution (split stage 3): the openjdk driver (maddi-run-openjdk, base) together
 * with the modification analysis it finds as a service (maddi-run-analysis, mod). The driver is base and cannot
 * carry the engine, so the launcher that ships lives here, in mod, and compiles nothing of its own.
 *
 *   ./gradlew :maddi-cli:run --args="…"          ./gradlew :maddi-cli:distZip   → maddi-<version>.zip
 */
plugins {
    id("java-library-conventions")
    application
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    runtimeOnly(project(":maddi-run-openjdk"))   // the driver; brings the analysis-hints archive (its runtimeOnly)
    runtimeOnly(project(":maddi-run-analysis"))  // the AnalysisEngine the driver loads
}

application {
    // launcher script `bin/maddi`, distribution `maddi-<version>.zip` (see PUBLISHING.md)
    applicationName = "maddi"
    mainClass = "io.codelaser.maddi.run.openjdkmain.Main"
    applicationDefaultJvmArgs = listOf(
        "-enableassertions", "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}

run {
    if (project.hasProperty("jvmArgs")) {
        application.applicationDefaultJvmArgs += (project.property("jvmArgs") as String).split("\\s+")
    }
}
