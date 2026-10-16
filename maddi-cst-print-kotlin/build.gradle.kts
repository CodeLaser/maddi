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
dependencies {
    api(project(":maddi-cst-api"))
    // the Kotlin printers reuse the OutputElement building blocks (OutputBuilderImpl, SymbolEnum, KeywordImpl,
    // TextImpl, …) and the import computation, exactly as the Java printers in cst-impl do
    implementation(project(":maddi-cst-impl"))
    // the analyzer's NULLABILITY_* properties: the default nullability verdicts
    api(project(":maddi-cst-analysis"))
    // what the Java-to-X printers share: print messages, the per-thread state, CST queries
    api(project(":maddi-cst-print-translate"))
    // the language-neutral formatter: KotlinTranslator writes files
    implementation(project(":maddi-cst-print"))

    // the tests print Java parsed by the openjdk front end (Java -> Kotlin translation)
    testImplementation(project(":maddi-inspection-api"))
    testImplementation(project(":maddi-inspection-openjdk"))
    testImplementation(project(":maddi-inspection-resource"))
}

tasks.test {
    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}
// The snippet tests' judge: kotlinc 2.4.0, the ratchets' compiler (see maddi-run-openjdk), in a child JVM.
// CommonJavaToKotlin.compiles(kotlin) checks that the printed Kotlin compiles, not only that it reads as intended.
val kotlinCompiler = configurations.create("kotlinCompiler") {
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    kotlinCompiler("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")
}
tasks.withType<Test>().configureEach {
    inputs.files(kotlinCompiler).withPropertyName("kotlinCompiler")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Dmaddi.test.kotlinCompilerClasspath=" + kotlinCompiler.asPath)
    })
}
