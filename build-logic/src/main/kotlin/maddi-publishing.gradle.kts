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

// A Maven publication for every maddi library, so that the sibling repositories can build against PUBLISHED jars
// instead of the source checkout (`-PmaddiFromSource=false` in maddi-mod and maddi-dist; split stage 6):
//     ./gradlew publishToMavenLocal                                   -- into ~/.m2
//     ./gradlew publishToMavenLocal -Dmaven.repo.local=/some/dir      -- into a repository of your choosing
// The java component carries the test-fixtures variants, and Gradle module metadata keeps them resolvable.
// java-library-conventions applies this to the base and mod tiers; a module that publishes on its own terms
// (the Central artefacts maddi-annotation and maddi-support, the build plugins) does not use it.

plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maddiLibrary") {
            from(components["java"])
        }
    }
}
