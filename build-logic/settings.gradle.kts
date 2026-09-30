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

// The conventions plugin (`java-library-conventions`) as an included build, not buildSrc: buildSrc cannot be
// consumed from another build, and maddi-mod and maddi-dist apply the same conventions
// (docs/roadmap/split-maddi-into-three-repositories.md, stage 4). Consumers:
//     pluginManagement { includeBuild("build-logic") }            -- this repository
//     pluginManagement { includeBuild("../maddi/build-logic") }   -- the sibling repositories
rootProject.name = "build-logic"
