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

    // Corpora skips (or, under -Dmaddi.corpus.required, fails) when a corpus is absent, so the
    // fixture needs the assumption API; the convention plugin only puts junit on `test`.
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api")
}
