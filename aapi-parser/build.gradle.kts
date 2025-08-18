/*
 * Copyright (c) 2022-2023, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */
plugins {
    id("java-library-conventions")
}

dependencies {
    api(project(":external-support"))
    api(project(":inspection-api"))
    implementation(project(":modification-common"))
    implementation(project(":modification-io"))
    implementation(project(":modification-prepwork"))
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

    implementation("ch.qos.logback:logback-classic")
    testImplementation(project(":modification-io"))

    testImplementation("org.apiguardian:apiguardian-api:1.1.2")
    testRuntimeOnly("info.picocli:picocli:4.7.7")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.springframework.security:spring-security-config:6.3.9")
    testRuntimeOnly("org.springframework.security:spring-security-web:6.3.9")
}


tasks.withType<Test> {
    maxHeapSize = "2G"
}