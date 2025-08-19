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
    implementation(project(":maddi-util"))
    implementation(project(":maddi-graph"))
    implementation(project(":maddi-cst-impl"))
    implementation(project(":maddi-cst-io"))
    implementation(project(":maddi-cst-analysis"))
    implementation(project(":maddi-cst-print"))
    implementation(project(":maddi-inspection-parser"))
    implementation(project(":maddi-inspection-resource"))
    implementation(project(":maddi-java-parser"))
    implementation(project(":maddi-java-bytecode"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-inspection-resource"))

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
