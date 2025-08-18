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
    api(project(":inspection-api"))
    implementation(project(":modification-common"))
    implementation(project(":internal-graph"))
    implementation(project(":internal-util"))
    implementation(project(":cst-analysis"))

    implementation(project(":cst-impl"))
    implementation(project(":cst-io"))
    implementation(project(":cst-print"))
    implementation(project(":inspection-parser"))
    implementation(project(":inspection-integration"))
    implementation(project(":inspection-resource"))
    implementation(project(":java-bytecode"))
    implementation(project(":java-parser"))

    testImplementation("ch.qos.logback:logback-classic")
}
tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4
}
