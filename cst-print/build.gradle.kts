/*
 * Copyright (c) 2022-2023, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */

plugins {
    id("java-library-conventions")
}

dependencies {
    api(project(":cst-api"))

    testImplementation(project(":cst-impl"))
}
