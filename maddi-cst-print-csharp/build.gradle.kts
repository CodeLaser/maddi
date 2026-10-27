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
    api(project(":maddi-cst-api"))
    // the C# printers reuse the OutputElement building blocks (OutputBuilderImpl, SymbolEnum, KeywordImpl,
    // TextImpl, …) and the import computation, exactly as the Java and Kotlin printers do
    implementation(project(":maddi-cst-impl"))
    // the analysis's verdicts stored on the declarations (CSharpFacts.STORED), as the Kotlin printer reads them
    api(project(":maddi-cst-analysis"))
    // what the Java-to-X printers share: print messages, the per-thread state, CST queries
    api(project(":maddi-cst-print-translate"))
    // the language-neutral formatter: CSharpTranslator writes files
    implementation(project(":maddi-cst-print"))

    // the tests print Java parsed by the openjdk front end (Java -> C# translation)
    testImplementation(project(":maddi-inspection-api"))
    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-inspection-resource"))
    // the library mappings' samples use slf4j, Jackson, JAXB and Micrometer (slf4j-api comes from java-library-conventions)
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("jakarta.xml.bind:jakarta.xml.bind-api")
    testImplementation("io.micrometer:micrometer-core")
}

tasks.test {
    // ./gradlew :maddi-cst-print-csharp:test -PupdateGolden rewrites the golden files (CommonJavaToCSharp.golden)
    systemProperty("csharp.golden.update", project.hasProperty("updateGolden"))
    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}
