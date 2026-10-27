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
