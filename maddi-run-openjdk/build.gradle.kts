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
    application
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-modification-analyzer"))
    implementation(project(":maddi-modification-link"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-openjdk"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-java-openjdk"))
    implementation(project(":maddi-java-parser"))
    implementation(project(":maddi-aapi-parser"))

    // to access resource:/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/libs.jar
    runtimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))
    implementation(project(":maddi-run-rewire"))

    implementation("commons-cli:commons-cli")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

application {
    // launcher script `bin/maddi`, distribution `maddi-<version>.zip` (see PUBLISHING.md)
    applicationName = "maddi"
    mainClass = "org.e2immu.analyzer.run.openjdkmain.Main"
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

tasks.test {
    useJUnitPlatform()
}


tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        )
    )
}

tasks.test {
    useJUnitPlatform()
    // -PnoAssertions disables JVM -ea; the linking engine's debug sanity assertions (consistencyCheck,
    // checkDuplicateNames) are not production behaviour, so turn them off to benchmark production-like linking.
    enableAssertions = !project.hasProperty("noAssertions")
    jvmArgs(
        "-Xmx6G",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}
