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
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    api(project(":maddi-inspection-api"))
    testImplementation(project(":maddi-annotation"))
    implementation(project(":maddi-cst-api"))
    implementation(project(":maddi-support"))
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


    implementation("ch.qos.logback:logback-classic")

    // libraries interpreted by maddi tests
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.springframework:spring-test:6.1.19")
    testImplementation("org.springframework:spring-core:6.1.19")
    testImplementation("org.projectlombok:lombok:1.18.48")
    // the tests PARSE sources that use Mockito (TestMethodCall13): input on the class path, not a use
    testRuntimeOnly("org.mockito:mockito-core:5.23.0")
}
tasks.withType<Test> {
    maxParallelForks = 4
}
