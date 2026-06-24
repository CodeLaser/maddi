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
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    testFixturesImplementation(project(":maddi-java-openjdk"))
    testFixturesImplementation(project(":maddi-inspection-resource"))
    testFixturesImplementation(project(":maddi-inspection-openjdk"))
    testFixturesImplementation("org.slf4j:slf4j-api")
    testFixturesImplementation("org.jetbrains:annotations")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
}
tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4
}
