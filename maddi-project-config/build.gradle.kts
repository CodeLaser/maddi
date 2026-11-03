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

// A project's analysis configuration without an IDE: the ways to make an inputConfiguration.json (a file a build
// plugin wrote, directories named by hand, a Gradle, Maven or any other compile log captured while the build runs),
// whether the build files that shaped it changed since, and how to make it again. Used by maddi-mcp and the IDE
// daemon (maddi-dist), and by any tool that keeps its projects its own way (ProjectRecords). Of maddi it needs the
// configuration types and the javac-log parser (maddi-run-config), nothing that parses or analyses. Base tier, so
// it publishes with every maddi release and pre-release: io.codelaser:maddi-project-config:<maddi version>.

plugins {
    id("java-library-conventions")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    api(project(":maddi-inspection-api"))             // InputConfiguration, BuildFiles
    implementation(project(":maddi-inspection-resource")) // BuildFilesCapture, InputConfigurationImpl
    implementation(project(":maddi-run-config"))      // ParseJavacList, JsonStreaming
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
