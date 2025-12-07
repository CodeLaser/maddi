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
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-inspection-parser"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-cst-io"))
    testImplementation(project(":maddi-cst-print"))
    testImplementation(project(":maddi-inspection-parser"))
    testImplementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-java-bytecode"))
    testImplementation(project(":maddi-java-parser"))
}
tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4
}
