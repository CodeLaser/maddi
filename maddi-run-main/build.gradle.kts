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
    implementation(project(":maddi-inspection-parser"))
    implementation(project(":maddi-inspection-integration"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-java-bytecode"))
    implementation(project(":maddi-java-parser"))
    implementation(project(":maddi-aapi-parser"))
    testRuntimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))

    implementation("commons-cli:commons-cli")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

application {
    mainClass = "org.e2immu.analyzer.run.main.Main"
 //   applicationDefaultJvmArgs = listOf("-enableassertions", "-Xmx24G")
}

run {
    if(project.hasProperty("jvmArgs")) {
        application.applicationDefaultJvmArgs = (project.property("jvmArgs") as String).split("\\s+")
    }
}

tasks.test {
    useJUnitPlatform()
}

