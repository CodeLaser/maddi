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
// What the Java-to-X printers (maddi-cst-print-kotlin, maddi-cst-print-csharp, maddi-cst-print-rust) share: the print messages, the
// per-thread state of a file's printing, and queries on the CST that do not depend on the target language.
dependencies {
    api(project(":maddi-cst-api"))
    // NullabilityFacts, which the use-site checks ask
    api(project(":maddi-cst-analysis"))
    // the OutputElement building blocks (OutputBuilderImpl, SymbolEnum, TextImpl, ...) the shared emission uses
    implementation(project(":maddi-cst-impl"))
    // Either, of TypeInfo.compilationUnitOrEnclosingType
    implementation(project(":maddi-support"))

    testImplementation(project(":maddi-cst-impl"))
}
