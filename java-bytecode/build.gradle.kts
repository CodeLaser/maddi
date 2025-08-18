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
    implementation("org.ow2.asm:asm:9.7.1")

    testImplementation(project(":cst-impl"))
    testImplementation(project(":inspection-resource"))
}
