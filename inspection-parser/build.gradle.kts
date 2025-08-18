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

    testImplementation(project(":cst-impl"))
    testImplementation(project(":inspection-resource"))
}
