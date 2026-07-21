/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
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

// A STANDALONE build: deliberately NOT listed in the root settings.gradle.kts, so nothing here can
// affect the normal maddi build. See README.md.

pluginManagement {
    repositories {
        // the plugin's own local file repository, filled by
        //   ./gradlew :maddi-gradleplugin:publishAllPublicationsToLocalPluginRepoRepository
        maven(url = uri("../maddi-gradleplugin/build/local-plugin-repo"))
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // the real build's own jars, consumed as ordinary module coordinates. A plain files(...) dependency
        // is NOT enough: the plugin walks resolved artifacts and only records those with a module or project
        // component identifier, so a file dependency never reaches the input configuration.
        listOf("maddi-support", "maddi-util", "maddi-cst-analysis").forEach {
            flatDir { dirs("../$it/build/libs") }
        }
    }
}

rootProject.name = "maddi-dogfood"

// One subproject per maddi module, mirroring the real module graph. cst-api and cst-impl must both be
// analyzed AS SOURCE: TypeInfo is an interface there, TypeInfoImpl implements it, and eventual immutability
// only travels between them when both are parsed (a jar type never enters the abstract-method batch).
// maddi-cst-analysis, maddi-support and maddi-util stay jars: they are below the interface/implementation
// pair we are testing, and maddi-support in particular must stay a jar so that reading the @Mark/@Only
// annotations out of BYTE CODE is exercised.
include("cst-api")
include("cst-impl")
