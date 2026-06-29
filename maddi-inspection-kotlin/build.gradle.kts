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

// The Kotlin front-end's driver/integration layer (the analogue of maddi-inspection-openjdk):
// turns an InputConfiguration into a parse, owns the shared InfoByFqn, and exposes a receptacle
// CompiledTypesManager that is a view over it. Written in Kotlin (it drives maddi-kotlin-k2).

plugins {
    kotlin("jvm") version "2.4.0"
}

group = "io.codelaser"

dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-cst-api"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-kotlin-k2"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-modification-prepwork")) // Tier-1: run the analyzer on Kotlin CST
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
