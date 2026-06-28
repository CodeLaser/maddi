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

package org.e2immu.language.inspection.kotlin

import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.inspection.api.resource.CompiledTypesManager
import org.e2immu.language.inspection.api.resource.SourceFile
import org.e2immu.language.inspection.resource.InfoByFqn

/**
 * The receptacle `CompiledTypesManager` for the Kotlin front-end. Unlike the openjdk one (a separate
 * map populated during the scan), this is simply a **view over the shared [InfoByFqn]** the scan writes
 * into — so there is one `TypeInfo` per (FQN, source set), and `get` returns exactly what the scan
 * resolved (see the `Info` identity invariant).
 */
class KotlinCompiledTypesManager(
    private val infoByFqn: InfoByFqn,
    private val javaBase: SourceSet,
    private val defaultSourceSet: SourceSet,
) : CompiledTypesManager {

    override fun javaBase(): SourceSet = javaBase

    override fun get(fullyQualifiedName: String, sourceSetOfRequest: SourceSet?): TypeInfo? =
        infoByFqn.getType(fullyQualifiedName, sourceSetOfRequest ?: defaultSourceSet)

    override fun typeDataOrNull(
        fqn: String,
        sourceSetOfRequest: SourceSet?,
        nearestSourceSet: SourceSet?,
        complainSingle: Boolean,
    ): CompiledTypesManager.TypeData? = null

    override fun addTypeInfo(sourceFile: SourceFile, typeInfo: TypeInfo) {
        // The registry is shared with the scan, which already registered this type; nothing to do.
    }
}
