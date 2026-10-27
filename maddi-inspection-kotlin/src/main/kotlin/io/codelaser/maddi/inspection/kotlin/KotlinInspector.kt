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
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.api.resource.InputConfiguration
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds
import io.codelaser.maddi.kotlin.api.KotlinParseObserver
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Driver for the Kotlin front-end — the analogue of `maddi-inspection-openjdk`'s `JavaInspectorImpl`.
 * It owns the one shared [InfoByFqn] registry, drives [KotlinFrontEnd] over the configured sources, and
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

    /** The Kotlin front end: the contract only. Its implementation may live in a realm of its own. */
    private val frontEnd get() = KotlinFrontEnds.get()

    /** Parse a set of Kotlin source files (name -> content) into the source set, sharing the registry. */
    fun parse(sourceSet: SourceSet, filesByName: Map<String, String>): List<TypeInfo> =
        frontEnd.sourceScan(runtime, sourceSet, infoByFqn, null).parse(filesByName)

    fun compiledTypesManager(): CompiledTypesManager = compiledTypesManager

    /**
     * Phase 4 — parse the whole [InputConfiguration] from disk: build ONE standalone K2 session over the
     * configured source **directories** and library **classpath** (not the running process's), with a
     * `KtSourceModule` per source set wired to its upstream source sets, then drive the front end per source set
     * in dependency order (so an upstream set's types are registered before a dependent references them),
     * sharing the one [infoByFqn]. Returns the committed types per source set. The analogue of how
     * `JavaInspectorImpl` consumes an `InputConfiguration`.
     */
    fun parseFromConfiguration(): Map<SourceSet, List<TypeInfo>> = parseFromConfiguration(emptyList())

    /**
     * As [parseFromConfiguration], with [observers] reading the parse through its K2 session before it closes
     * (e.g. a [io.codelaser.maddi.kotlin.api.KotlinReferenceIndex], from [KotlinFrontEnd.referenceIndex]).
     */
    fun parseFromConfiguration(observers: List<KotlinParseObserver>): Map<SourceSet, List<TypeInfo>> {
        val ordered = dependencyOrder(inputConfiguration.sourceSets().filter { !it.externalLibrary() })
        // library jars from the configuration's classpath (external, non-JDK, on disk); the JDK comes from the SDK module
        val libraryRoots = inputConfiguration.classPathParts()
            .filter { it.externalLibrary() && !it.partOfJdk() }
            .mapNotNull { uriToPath(it.uri()) }
            .filter { Files.exists(it) }
        val jdkHome = Paths.get(System.getProperty("java.home"))
        return frontEnd.projectScan(runtime, infoByFqn, null)
            .parse(ordered, libraryRoots, jdkHome, emptyList(), observers)
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
