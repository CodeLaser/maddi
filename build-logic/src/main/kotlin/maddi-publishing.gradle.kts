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
