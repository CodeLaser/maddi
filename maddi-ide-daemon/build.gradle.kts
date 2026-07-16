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
    // analysis pipeline (mirrors maddi-run-main)
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))   // DecoratorImpl, PrepAnalyzer, ComputeCallGraph, ComputeAnalysisOrder
    implementation(project(":maddi-modification-analyzer"))    // IteratingAnalyzerImpl
    implementation(project(":maddi-modification-link"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))             // PropertyImpl keys, ValueImpl
    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-openjdk"))       // JavaInspectorImpl (the integration one is phased out)
    implementation(project(":maddi-inspection-resource"))      // InputConfigurationImpl
    implementation(project(":maddi-java-bytecode"))
    implementation(project(":maddi-aapi-parser"))

    // to access resource:/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/libs.jar
    runtimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))               // ErrorReport, JsonStreaming reuse

    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // the e2immu annotations, supplied to every analyzed project as a classpath part (SourceSetImpl.sourceSetOf)
    implementation(project(":maddi-support"))
}

// The openjdk inspector drives javac internals; these exports are required at runtime (see maddi-run-openjdk).
val openjdkExports = listOf(
    "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
)

application {
    mainClass = "org.e2immu.analyzer.ide.daemon.DaemonMain"
    // baked into the generated start script's DEFAULT_JVM_OPTS, so the launched daemon has them
    applicationDefaultJvmArgs = openjdkExports
}

// The analyze test points a real on-disk project at the e2immu annotations (maddi-support, on the test
// classpath) as its "hot class files", exactly as the plugin will point maddi at IntelliJ's output.
tasks.test {
    jvmArgs(openjdkExports)
    useJUnitPlatform()
}
