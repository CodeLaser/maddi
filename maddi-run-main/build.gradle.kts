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

plugins {
    id("java-library-conventions")
    application
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    implementation(project(":maddi-analysis-api"))  // the modification analysis, as a service (split stage 3)
    implementation(project(":maddi-cst-api"))
    implementation(project(":maddi-callgraph"))  // ComputeCallGraph & co., moved out of prepwork (split stage 2)
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))

    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-inspection-integration"))
    implementation(project(":maddi-inspection-resource"))

    // to access resource:/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/libs.jar
    runtimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))
    implementation(project(":maddi-run-rewire"))

    implementation("commons-cli:commons-cli")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

application {
    mainClass = "io.codelaser.maddi.run.main.Main"
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

