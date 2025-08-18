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
    implementation(project(":maddi-util"))
    implementation(project(":maddi-graph"))

    testImplementation(project(":maddi-cst-impl"))
    testImplementation(project(":maddi-inspection-resource"))
}
