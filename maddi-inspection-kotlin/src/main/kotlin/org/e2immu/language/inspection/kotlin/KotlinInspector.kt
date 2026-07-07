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
import org.e2immu.language.kotlin.k2.KotlinProjectScan
import org.e2immu.language.kotlin.k2.KotlinScan
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

    /**
     * Phase 4 — parse the whole [InputConfiguration] from disk: build ONE standalone K2 session over the
     * configured source **directories** and library **classpath** (not the running process's), with a
     * `KtSourceModule` per source set wired to its upstream source sets, then drive [KotlinScan] per source set
     * in dependency order (so an upstream set's types are registered before a dependent references them),
     * sharing the one [infoByFqn]. Returns the committed types per source set. The analogue of how
     * `JavaInspectorImpl` consumes an `InputConfiguration`.
     */
    fun parseFromConfiguration(): Map<SourceSet, List<TypeInfo>> {
        val ordered = dependencyOrder(inputConfiguration.sourceSets().filter { !it.externalLibrary() })
        // library jars from the configuration's classpath (external, non-JDK, on disk); the JDK comes from the SDK module
        val libraryRoots = inputConfiguration.classPathParts()
            .filter { it.externalLibrary() && !it.partOfJdk() }
            .mapNotNull { uriToPath(it.uri()) }
            .filter { Files.exists(it) }
        val jdkHome = Paths.get(System.getProperty("java.home"))
        return KotlinProjectScan(runtime, infoByFqn).parse(ordered, libraryRoots, jdkHome)
    }

    /** Topological order (dependencies before dependents) over the given source sets; ignores library deps. */
    private fun dependencyOrder(sourceSets: List<SourceSet>): List<SourceSet> {
        val set = sourceSets.toSet()
        val ordered = LinkedHashSet<SourceSet>()
        fun visit(ss: SourceSet) {
            if (ss in ordered || ss !in set) return
            ss.dependencies().forEach { if (it in set) visit(it) }
            ordered.add(ss)
        }
        sourceSets.forEach { visit(it) }
        return ordered.toList()
    }

    private fun uriToPath(uri: java.net.URI): Path? =
        runCatching { if (uri.scheme == "file") Paths.get(uri) else Paths.get(uri.schemeSpecificPart) }.getOrNull()
}
