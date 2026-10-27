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
/*
Deliberately no dependency on an inspector implementation: the driver works through the JavaInspector interface
alone, which is what lets both run modules share it. Adding one here would defeat the point.
Every dependency below is one both maddi-run-main and maddi-run-openjdk already declare, so this module adds a node
to the build graph but no new reachability.
 */
dependencies {
    implementation(project(":maddi-callgraph"))  // ComputeCallGraph & co., moved out of prepwork (split stage 2)
    implementation(project(":maddi-cst-api"))
    api(project(":maddi-inspection-api"))                   // JavaInspector, ParseResult, InputConfiguration
    implementation(project(":maddi-graph"))                 // G<Info>
    // slf4j-api comes from java-library-conventions
}
