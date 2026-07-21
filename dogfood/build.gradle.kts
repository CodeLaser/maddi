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

/*
 One subproject per maddi module, each pointing at that module's real source directory. The modules are
 NOT merged: every maddi module is a JPMS module, and several module-info.java in one compilation collide
 ("too many module declarations"); dropping them instead makes javac compile the merge as one of the
 modules, and every `requires`d package then comes back as "package org.slf4j is not visible".

 cst-api and cst-impl are both analyzed as source because that is the whole point: TypeInfo is an
 interface in cst-api, TypeInfoImpl implements it in cst-impl, and eventual immutability can only travel
 between them when both are parsed -- a jar type never enters the abstract-method batch. The plugin's
 e2immuSourceElements variant is what carries cst-api's sources into cst-impl's input configuration.
*/
plugins {
    // resolved once here, applied in the subprojects
    id("org.e2immu.analyzer-plugin") version "0.8.2" apply false
}
