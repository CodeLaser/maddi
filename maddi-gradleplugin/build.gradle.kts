/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


plugins {
    `java-gradle-plugin`
    id("java-library-conventions")
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}
dependencies {
    api(project(":maddi-inspection-api"))
    implementation(project(":maddi-modification-common"))
    implementation(project(":maddi-modification-io"))
    implementation(project(":maddi-modification-prepwork"))
    implementation(project(":maddi-modification-linkedvariables"))
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
    implementation(project(":maddi-aapi-parser"))
    testRuntimeOnly(project(":maddi-aapi-archive"))

    implementation(project(":maddi-run-config"))
    implementation(project(":maddi-run-main"))

    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // GRADLE PLUGIN
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("e2immuAnalyzerPlugin") {
            id = "org.e2immu.analyzer-plugin"
            implementationClass = "org.e2immu.gradleplugin.AnalyzerPlugin"
            displayName = "e2immu's gradle plugin"
        }
        description = "Run the e2immu analyzer from Gradle"
        isAutomatedPublishing = true
    }
}
