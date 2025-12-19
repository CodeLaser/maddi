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


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include("maddi-aapi-archive")
include("maddi-aapi-parser")
include("maddi-cst-analysis")
include("maddi-cst-api")
include("maddi-cst-impl")
include("maddi-cst-io")
include("maddi-cst-print")
include("maddi-support")
include("maddi-inspection-api")
include("maddi-inspection-integration")
include("maddi-inspection-parser")
include("maddi-inspection-resource")
include("maddi-graph")
include("maddi-util")
include("maddi-java-parser")
include("maddi-java-bytecode")
include("maddi-modification-analyzer")
include("maddi-modification-common")
include("maddi-modification-io")
include("maddi-modification-link")
include("maddi-modification-prepwork")
include("platform")
include("road-to-immutability")
include("maddi-run-config")
//include("maddi-gradleplugin")
include("maddi-run-main")
//include("maddi-mvnplugin")
//include("testgradleplugin-analyzer")
//include("testgradleplugin-writeaapi")
//include("testmvnplugin-export")
