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
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.inspection.api.resource.CompiledTypesManager
import org.e2immu.language.inspection.api.resource.InputConfiguration
import org.e2immu.language.inspection.resource.InfoByFqn
import org.e2immu.language.kotlin.k2.KotlinScan

/**
 * Driver for the Kotlin front-end — the analogue of `maddi-inspection-openjdk`'s `JavaInspectorImpl`.
 * It owns the one shared [InfoByFqn] registry, drives [KotlinScan] over the configured sources, and
 * exposes a [CompiledTypesManager] that is a view over that same registry. (Multi-source-set ordering,
 * the full `JavaInspector` surface, and a classpath taken from the [InputConfiguration] rather than the
 * running process are later steps; see kotlin-parser-plan.md.)
 */
class KotlinInspector(private val runtime: Runtime) {

    private lateinit var inputConfiguration: InputConfiguration
    private val infoByFqn = InfoByFqn()
    private lateinit var compiledTypesManager: CompiledTypesManager

    fun initialize(inputConfiguration: InputConfiguration) {
        this.inputConfiguration = inputConfiguration
        // javaBase() throws when no "java.base" classpath part is configured (e.g. minimal setups)
        val javaBase = runCatching { inputConfiguration.javaBase() }.getOrNull()
        val mainSourceSet = inputConfiguration.sourceSets().firstOrNull() ?: javaBase
        ?: error("KotlinInspector.initialize: no source sets and no java.base")
        compiledTypesManager = KotlinCompiledTypesManager(infoByFqn, javaBase ?: mainSourceSet, mainSourceSet)
    }

    /** Parse a set of Kotlin source files (name -> content) into the source set, sharing the registry. */
    fun parse(sourceSet: SourceSet, filesByName: Map<String, String>): List<TypeInfo> =
        KotlinScan(runtime, sourceSet, infoByFqn).parse(filesByName)

    fun compiledTypesManager(): CompiledTypesManager = compiledTypesManager
}
