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

// Everything this build consumes from maddi -- the analyzer plugin, and the maddi-support / maddi-util
// jars -- is pinned at the PROJECT's own version, read here from the real build's gradle.properties (a
// standalone build does not inherit it). No version literal lives anywhere under dogfood/.
//
// This is not tidiness. The jar pins used to carry a frozen 0.8.2 while the project stood at 0.9.0, and a
// frozen literal is a hole in the ratchet: TestEventualRatchet then measures whatever those two modules
// looked like at some past release, so a change to either reads as "no change" rather than as a
// regression. `./gradlew build` in the parent must have produced all of these at this version; if it has
// not, resolution fails loudly here rather than quietly serving the old ones.
//
// The read sits inside pluginManagement because that block has to come first in a settings script, and
// the plugin version is one of the things it feeds.
pluginManagement {
    val maddiVersion: String = java.util.Properties().apply {
        settingsDir.resolve("../gradle.properties").inputStream().use { load(it) }
    }.getProperty("version") ?: error("no `version` in ../gradle.properties")
    gradle.beforeProject { extra["maddiVersion"] = maddiVersion }

    repositories {
        // the plugin's own local file repository, filled by
        //   ./gradlew :maddi-gradleplugin:publishAllPublicationsToLocalPluginRepoRepository
        maven(url = uri("../maddi-gradleplugin/build/local-plugin-repo"))
        gradlePluginPortal()
    }
    // the plugin is requested without a version in build.gradle.kts; it gets the project's
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.e2immu.analyzer-plugin") useVersion(maddiVersion)
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // the real build's own jars, consumed as ordinary module coordinates. A plain files(...) dependency
        // is NOT enough: the plugin walks resolved artifacts and only records those with a module or project
        // component identifier, so a file dependency never reaches the input configuration.
        listOf("maddi-support", "maddi-util").forEach {
            flatDir { dirs("../$it/build/libs") }
        }
    }
}

rootProject.name = "maddi-dogfood"

// One subproject per maddi module, mirroring the real module graph. cst-api and cst-impl must both be
// analyzed AS SOURCE: TypeInfo is an interface there, TypeInfoImpl implements it, and eventual immutability
// only travels between them when both are parsed (a jar type never enters the abstract-method batch).
// cst-analysis is also analyzed as source: it holds PropertyValueMapImpl, whose getOrDefault/getOrNull must
// be COMPUTED non-modifying for ParameterInfoImpl's analysis store to stop capping it (a jar leaves them
// unproven). maddi-support and maddi-util stay jars: maddi-support in particular stays a jar so that reading
// the @Mark/@Only annotations out of BYTE CODE is exercised.
include("cst-api")
include("cst-analysis")
include("cst-impl")
