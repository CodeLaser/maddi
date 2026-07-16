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

// The IDE-agnostic daemon client: DTOs (AnalysisModel), the NDJSON socket client, the process launcher.
// Plain JDK + Jackson only — NO IntelliJ and NO maddi types — so any IDE front-end can share it. The
// IntelliJ plugin depends on it; an Eclipse plugin (also a JVM) can too. A VS Code extension reuses only
// the JSON wire contract (reimplemented in TypeScript). Targets Java 21, the floor of the IDE runtimes
// that consume it (IntelliJ's JBR 21, Eclipse's JRE 21+).

plugins {
    id("java-library-conventions")
    `maven-publish` // so an Eclipse/Tycho build can consume this jar from the local Maven repo (publishToMavenLocal)
}

version = "0.8.2"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"]) // io.codelaser:maddi-ide-client:0.8.2
        }
    }
}

dependencies {
    // exposed on the API: consumers deserialize daemon frames into AnalysisModel via Jackson (JsonNode etc.)
    api("com.fasterxml.jackson.core:jackson-databind")
}

// The launcher/round-trip tests drive the real daemon distribution (installDist: bin/ + lib/*.jar),
// exactly as an IDE front-end will. Provide its location and a JDK 25+ to run it on (the daemon needs 25).
tasks.test {
    dependsOn(":maddi-ide-daemon:installDist")
    val installDir = project(":maddi-ide-daemon").layout.buildDirectory.dir("install/maddi-ide-daemon")
    systemProperty("maddi.daemon.install", installDir.get().asFile.absolutePath)
    systemProperty("maddi.test.jdkHome", System.getProperty("java.home"))
}
