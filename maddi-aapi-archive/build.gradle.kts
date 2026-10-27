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
    api(project(":maddi-support"))
    implementation(project(":maddi-annotation"))
    // to express contracts for Kotlin library types (kotlin.Pair): parsed, never run -- hence compileOnly
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
    // the side-loaded vavr hints (libs/vavr) name vavr types in their signatures; parsed, never run
    compileOnly("io.vavr:vavr:1.0.1")
    // the same for the side-loaded Eclipse Collections hints (libs/eclipsecollections)
    compileOnly("org.eclipse.collections:eclipse-collections-api:13.0.0")
    compileOnly("org.eclipse.collections:eclipse-collections:13.0.0")
    // and for the Guava hints (libs/guava, in libs.jar); the corpus is a 2026-07 HEAD, 33.6.0 is the release before it
    compileOnly("com.google.guava:guava:33.6.0-jre")
    compileOnly("org.assertj:assertj-core:3.27.7") // libs/assertj (CodeLaser/maddi#116)
    compileOnly("org.hamcrest:hamcrest:2.2") // libs/hamcrest
    compileOnly("org.junit.jupiter:junit-jupiter-params:6.0.3") // libs/junitparams
    compileOnly("org.mockito:mockito-core:5.23.0") // libs/mockito
    compileOnly("commons-io:commons-io:2.20.0") // libs/commonsio
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.20.2") // libs/jackson
    implementation("org.junit.jupiter:junit-jupiter-api")
    implementation("ch.qos.logback:logback-classic")
}
