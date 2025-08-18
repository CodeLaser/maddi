/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}
dependencies {
    api(project(":inspection-api"))
    implementation(project(":modification-common"))
    implementation(project(":modification-io"))
    implementation(project(":modification-prepwork"))
    implementation(project(":modification-linkedvariables"))
    implementation(project(":internal-graph"))
    implementation(project(":internal-util"))
    implementation(project(":cst-analysis"))

    implementation(project(":cst-impl"))
    implementation(project(":cst-io"))
    implementation(project(":cst-print"))
    implementation(project(":inspection-parser"))
    implementation(project(":inspection-integration"))
    implementation(project(":inspection-resource"))
    implementation(project(":java-bytecode"))
    implementation(project(":java-parser"))
    implementation(project(":aapi-parser"))
    testRuntimeOnly(project(":aapi-archive"))

    implementation(project(":run-config"))

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

