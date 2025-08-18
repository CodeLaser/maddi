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
    implementation(project(":internal-util"))
    implementation(project(":internal-graph"))
    implementation(project(":cst-impl"))
    implementation(project(":cst-io"))
    implementation(project(":cst-analysis"))
    implementation(project(":cst-print"))
    implementation(project(":inspection-parser"))
    implementation(project(":inspection-resource"))
    implementation(project(":java-parser"))
    implementation(project(":java-bytecode"))

    testImplementation(project(":cst-impl"))
    testImplementation(project(":inspection-resource"))

    implementation("ch.qos.logback:logback-classic")

    // libraries interpreted by maddi tests
    testImplementation("org.apiguardian:apiguardian-api:1.1.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.springframework:spring-test:6.1.19")
    testImplementation("org.springframework:spring-core:6.1.19")
}

tasks.withType<Test> {
    maxParallelForks = 4
}
