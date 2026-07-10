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
    api(project(":maddi-support"))
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-parser"))
    implementation(project(":maddi-inspection-resource"))

    implementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-java-bytecode"))
    testImplementation(project(":maddi-java-parser"))

    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-java-openjdk"))
    testImplementation(testFixtures(project(":maddi-modification-common")))

    implementation("ch.qos.logback:logback-classic")

    testImplementation("org.apiguardian:apiguardian-api:1.1.2")
    testRuntimeOnly("info.picocli:picocli:4.7.7")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.springframework.security:spring-security-config:6.3.9")
    testRuntimeOnly("org.springframework.security:spring-security-web:6.3.9")

}


// the openjdk front-end's javac internals need these exports (shared by the Test tasks and the compile task)
val javacAddExports = listOf(
    "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
)

// Regenerate the analysis-result (.json) files in maddi-aapi-archive from the hand-written hints, without
// running it as a test: ./gradlew :maddi-aapi-parser:compileAnalysisHints
tasks.register<JavaExec>("compileAnalysisHints") {
    group = "e2immu"
    description = "Compile the analysis hints (maddi-aapi-archive) into their analysis-result JSON files"
    dependsOn(tasks.named("testClasses")) // CompileAnalysisHints + its factory live in the test source set
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("org.e2immu.analyzer.aapi.parser.CompileAnalysisHints")
    workingDir = projectDir // paths in CompileAnalysisHints are relative to this module directory
    maxHeapSize = "2G"
    jvmArgs(javacAddExports)
}

tasks.withType<Test> {
    maxHeapSize = "2G"

    jvmArgs(javacAddExports)

    val impl = System.getProperty("maddi_parser", "maddi")

    // Pass it forward down to the worker JVM execution context
    systemProperty("maddi_parser", impl)

    // Visual logging to your terminal so you always know which version is active
    logger.lifecycle("Project [${project.name}] executing test suite targeting: $impl")
}