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
/*
Deliberately no dependency on an inspector implementation: the driver works through the JavaInspector interface
alone, which is what lets both run modules share it. Adding one here would defeat the point.
Every dependency below is one both maddi-run-main and maddi-run-openjdk already declare, so this module adds a node
to the build graph but no new reachability.
 */
dependencies {
    api(project(":maddi-inspection-api"))                   // JavaInspector, ParseResult, InputConfiguration
    implementation(project(":maddi-modification-prepwork")) // ComputeCallGraph, PrimaryTypeUseGraph
    implementation(project(":maddi-graph"))                 // G<Info>
    // slf4j-api comes from java-library-conventions
}
