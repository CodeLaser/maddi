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

/*
 * The service interface to the modification analysis (split stage 3): the base tier and the ext tier compile
 * against this; maddi-run-analysis (maddi-mod) implements it and arrives at run time. See AnalysisEngine.
 */
plugins {
    id("java-library-conventions")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    // every one of these appears in a signature of this module
    api(project(":maddi-cst-api"))
    api(project(":maddi-inspection-api"))
    api(project(":maddi-graph"))
    api(project(":maddi-callgraph"))
    api(project(":maddi-util"))
    implementation(project(":maddi-support"))  // Either, in AnalysisHintsShadows
}
