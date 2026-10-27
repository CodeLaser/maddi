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
    implementation(project(":maddi-java-parser"))
    testImplementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-resource"))

    testImplementation(project(":maddi-cst-impl"))

    testImplementation("org.projectlombok:lombok:1.18.48")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.48")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=io.codelaser.maddi.java.openjdk,ALL-UNNAMED"
        )
    )
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 4
    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=io.codelaser.maddi.java.openjdk,ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=io.codelaser.maddi.java.openjdk,ALL-UNNAMED"
    )
}