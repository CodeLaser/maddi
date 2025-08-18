/*
 * Copyright (c) 2022-2023, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */
plugins {
    id("java-library-conventions")
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}
dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-parser"))
    implementation(project(":maddi-inspection-integration"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-java-bytecode"))
    implementation(project(":maddi-java-parser"))

    testImplementation("ch.qos.logback:logback-classic")
}
tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4
}
