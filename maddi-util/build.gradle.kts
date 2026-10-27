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

plugins {
   id("java-library-conventions")
   // Corpora: the one corpus locator, shared by every repository's corpus tests. It is here rather
   // than in a runner module because every consuming repo already depends on maddi-util, and
   // resolving a directory must not drag a 43-file runner onto a test's class path.
   `java-test-fixtures`
}
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    api(project(":maddi-support"))
    implementation(project(":maddi-annotation"))

    // Corpora skips (or, under -Dmaddi.corpus.required, fails) when a corpus is absent, so the
    // fixture needs the assumption API; the convention plugin only puts junit on `test`.
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
}
