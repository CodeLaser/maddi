/*
 * Copyright (c) 2022-2023, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */
plugins {
    id("java-library-conventions")
}

dependencies {
    api(project(":inspection-api"))
    implementation(project(":internal-graph"))
    implementation(project(":internal-util"))
    implementation(project(":cst-analysis"))

    testImplementation(project(":cst-impl"))
    testImplementation(project(":cst-io"))
    testImplementation(project(":cst-print"))
    testImplementation(project(":inspection-parser"))
    testImplementation(project(":inspection-integration"))
    testImplementation(project(":inspection-resource"))
    testImplementation(project(":java-bytecode"))
    testImplementation(project(":java-parser"))
}


tasks.withType<Test> {
    maxHeapSize = "2G"
    maxParallelForks = 4
}
