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
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-analysis"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-parser"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-java-parser"))
    implementation(project(":maddi-java-bytecode"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-inspection-resource"))

    implementation("ch.qos.logback:logback-classic")

    // libraries interpreted by maddi tests
    testImplementation("org.apiguardian:apiguardian-api:1.1.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.springframework:spring-test:6.1.19")
    testImplementation("org.springframework:spring-core:6.1.19")
    testImplementation("org.projectlombok:lombok:1.18.42")
}
tasks.withType<Test> {
    maxParallelForks = 4
}
