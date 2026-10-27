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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.api.resource.SourceFile
import io.codelaser.maddi.inspection.resource.InfoByFqn

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

    override fun typeIfLoaded(fullyQualifiedName: String, sourceSetOfRequest: SourceSet?): TypeInfo? =
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
