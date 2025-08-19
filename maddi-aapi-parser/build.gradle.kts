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
    api(project(":maddi-support"))
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-io"))
    implementation(project(":maddi-modification-prepwork"))
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

    implementation("ch.qos.logback:logback-classic")
    testImplementation(project(":maddi-modification-io"))

    testImplementation("org.apiguardian:apiguardian-api:1.1.2")
    testRuntimeOnly("info.picocli:picocli:4.7.7")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.springframework.security:spring-security-config:6.3.9")
    testRuntimeOnly("org.springframework.security:spring-security-web:6.3.9")
}


tasks.withType<Test> {
    maxHeapSize = "2G"
}