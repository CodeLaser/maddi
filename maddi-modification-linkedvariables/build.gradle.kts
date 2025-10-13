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
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}
dependencies {
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
    implementation(project(":maddi-inspection-integration"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-java-bytecode"))
    implementation(project(":maddi-java-parser"))

    testImplementation("org.apiguardian:apiguardian-api:1.1.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("ch.qos.logback:logback-classic")
    testImplementation(project(":maddi-modification-io"))

    testRuntimeOnly(project(":maddi-aapi-archive"))
}
tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4
}
