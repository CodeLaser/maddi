/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


// Type-safe accessors aren't generated for this settings-plugin extension inside the nested
// dependencyResolutionManagement.repositories scope, so import the extension function explicitly.
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    // `java-library-conventions` (formerly buildSrc): an included build, so that the sibling repositories of the
    // split can apply the same conventions (docs/roadmap/split-maddi-into-three-repositories.md, stage 4)
    includeBuild("build-logic")
}

plugins {
    // Adds the IntelliJ Platform repositories at the settings level, required because
    // repositoriesMode is FAIL_ON_PROJECT_REPOS (no per-project repositories allowed). See maddi-intellij.
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // Kotlin K2 Analysis API ('*-for-ide' artifacts) — not on Maven Central. See maddi-kotlin-k2.
        maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
        maven(url = "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies")
        // IntelliJ IDEA SDK + marketplace, for the maddi-intellij plugin module.
        intellijPlatform {
            defaultRepositories()
        }
    }
}

include("maddi-aapi-archive")
include("maddi-cst-analysis")
include("maddi-cst-api")
include("maddi-cst-impl")
include("maddi-cst-io")
include("maddi-cst-print")
include("maddi-annotation")
include("maddi-support")
include("maddi-inspection-api")
include("maddi-inspection-integration")
include("maddi-inspection-parser")
include("maddi-inspection-resource")
include("maddi-inspection-openjdk")
include("maddi-graph")
include("maddi-callgraph")
include("maddi-analysis-api")
include("maddi-util")
include("maddi-java-parser")
include("maddi-java-openjdk")
include("maddi-java-bytecode")
include("maddi-kotlin-api")
include("maddi-kotlin-k2")
include("maddi-kotlin-realm")
include("maddi-inspection-kotlin")
include("maddi-inspection-mixed")
include("platform")
include("road-to-immutability")
include("maddi-manual")
include("maddi-run-config")
include("maddi-run-rewire")
include("maddi-run-main")
include("maddi-run-openjdk")
include("maddi-run-kotlin")
include("maddi-cst-print-translate")
include("maddi-cst-print-kotlin")
include("maddi-cst-print-csharp")
include("maddi-cst-print-rust")
//include("maddi-run-scanbuild")
