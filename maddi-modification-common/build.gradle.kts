/*
 * Copyright (c) 2022-2023, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */
plugins {
    id("java-library-conventions")
}
java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}
dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-util"))
    implementation(project(":maddi-cst-analysis"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-cst-io"))
    testImplementation(project(":maddi-cst-print"))
    testImplementation(project(":maddi-inspection-parser"))
    testImplementation(project(":maddi-inspection-integration"))
    testImplementation(project(":maddi-inspection-resource"))
    testImplementation(project(":maddi-java-bytecode"))
    testImplementation(project(":maddi-java-parser"))
}
tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4
}
