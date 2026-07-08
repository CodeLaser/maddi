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

package org.e2immu.language.inspection.mixed

import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.inspection.api.integration.JavaInspector
import org.e2immu.language.inspection.api.resource.InputConfiguration
import org.e2immu.language.inspection.kotlin.JavaStubGenerator
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinProjectScan
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider

/**
 * The **multi-source-set** mixed driver: parse a mixed Java+Kotlin [InputConfiguration] so every type lands in
 * its own CST source set (not flattened into one, as [MixedInspector.parseFromConfiguration] does), over one
 * shared core. Each source set is classified by its on-disk contents (`.kt` ⇒ Kotlin, else `.java` ⇒ Java).
 *
 * The openjdk [JavaInspectorImpl] owns the shared `Runtime`/`InfoByFqn`/`CompiledTypesManager` and is
 * initialised with the Java source sets (+ the generated-stub directory on their classpath). The parse order
 * follows the cross-language dependency direction:
 * - **Java→Kotlin** (or independent): Kotlin runs first via [KotlinProjectScan] (one `KaSourceModule` per set,
 *   dependency order, sharing the core); a stub is generated and compiled for every Kotlin type; then the Java
 *   sets parse from disk, resolving Kotlin references against the stubs and reusing the shared Kotlin `TypeInfo`.
 * - **Kotlin→Java** (only): the Java sets parse first (their source types commit to the shared CTM), then
 *   Kotlin resolves those references to the same instances ([KotlinProjectScan] gets the Java dirs as source
 *   roots so K2 resolves the symbols; the `TypeInfo` comes from the shared CTM).
 *
 * Current scope: a Java set's Kotlin-set dependencies are satisfied via the stubs (dropped from the Java
 * inspector's view). Java→Java dependencies across withDependencies-rebuilt sets, and a project that mixes
 * *both* cross-language directions (an intra-module Kotlin↔Java cycle — the skeleton-pre-pass case), are
 * follow-ups. Single-threaded (javac); needs the openjdk `--add-exports`.
 */
class MixedProjectInspector {

    private val stubDir = Files.createTempDirectory("mixed-proj-stubs")

    /** [kotlinBySourceSet] keeps the per-source-set placement; [javaTypes] are the primary Java types. */
    data class Result(val kotlinBySourceSet: Map<SourceSet, List<TypeInfo>>, val javaTypes: List<TypeInfo>) {
        val kotlinTypes: List<TypeInfo> get() = kotlinBySourceSet.values.flatten()
    }

    fun parse(config: InputConfiguration): Result {
        val sourceSets = config.sourceSets().filter { !it.externalLibrary() }
        val kotlinSets = sourceSets.filter { hasExtension(it, ".kt") }
        val javaSets = sourceSets.filter { !hasExtension(it, ".kt") && hasExtension(it, ".java") }
        val javaSetIdentity = javaSets.toSet()
        val kotlinSetIdentity = kotlinSets.toSet()

        val stubSet: SourceSet = SourceSetImpl.Builder().setName("mixed-stubs")
            .setUri(stubDir.toUri()).setExternalLibrary(true).build()

        // the Java front-end owns the shared core; a Java set's Kotlin-set deps are satisfied by the stubs (so
        // dropped), its library + Java-set deps are kept, plus the stub directory. withDependencies() mints new
        // SourceSet instances, so rebuild in dependency order and remap a Java-set dep to its rebuilt instance —
        // otherwise a dependent would point at the original (not-in-config) set and the linearization misses it.
        val javaInspector = JavaInspectorImpl()
        val javaConfig = InputConfigurationImpl.Builder().addClassPath("jmod:java.base").addClassPathParts(stubSet)
        val rebuiltJavaSet = LinkedHashMap<SourceSet, SourceSet>()
        dependencyOrder(javaSets).forEach { js ->
            val keptDeps = js.dependencies().mapNotNull { d ->
                when {
                    d.externalLibrary() -> d
                    d in javaSetIdentity -> rebuiltJavaSet[d] ?: d // the rebuilt upstream Java set
                    else -> null // a Kotlin-set dependency: satisfied by the stubs
                }
            }
            val rebuilt = js.withDependencies(keptDeps + stubSet)
            rebuiltJavaSet[js] = rebuilt
            javaConfig.addSourceSets(rebuilt)
        }
        javaInspector.initialize(javaConfig.build())
        javaInspector.onlyPreload() // warm the CTM lazy loader before Kotlin delegates java.* to it

        val runtime = javaInspector.runtime()
        val infoByFqn = javaInspector.infoByFqn()
        val ctm = javaInspector.compiledTypesManager()
        val libraryRoots = config.classPathParts()
            .filter { it.externalLibrary() && !it.partOfJdk() }
            .mapNotNull { uriToPath(it.uri()) }.filter { Files.exists(it) }
        val jdkHome = Paths.get(System.getProperty("java.home"))
        val orderedKotlin = dependencyOrder(kotlinSets)
        val options = JavaInspector.ParseOptions.Builder().build()

        val kotlinDependsOnJava = kotlinSets.any { it.dependencies().any { d -> d in javaSetIdentity } }
        val javaDependsOnKotlin = javaSets.any { it.dependencies().any { d -> d in kotlinSetIdentity } }

        if (kotlinDependsOnJava && !javaDependsOnKotlin) {
            // Kotlin→Java only: parse Java first (its source types commit to the shared CTM), then Kotlin
            // resolves those references to the same instances (K2 sees the Java dirs as source roots).
            val javaTypes = javaInspector.parse(mapOf(), options).parseResult().primaryTypes().toList()
            val javaSourceRoots = javaSets.flatMap { it.sourceDirectories() }
            val kotlinBySourceSet = KotlinProjectScan(runtime, infoByFqn, ctm)
                .parse(orderedKotlin, libraryRoots, jdkHome, javaSourceRoots)
            return Result(kotlinBySourceSet, javaTypes)
        }

        // Java→Kotlin (or independent): Kotlin first, generate stubs, then Java resolves Kotlin via the stubs.
        val kotlinBySourceSet = KotlinProjectScan(runtime, infoByFqn, ctm).parse(orderedKotlin, libraryRoots, jdkHome)
        val kotlinTypes = kotlinBySourceSet.values.flatten()
        if (kotlinTypes.isNotEmpty()) {
            compileStubs(kotlinTypes.associate { it.fullyQualifiedName() to JavaStubGenerator.stub(it) })
        }
        val javaTypes = javaInspector.parse(mapOf(), options).parseResult().primaryTypes().toList()
        return Result(kotlinBySourceSet, javaTypes)
    }

    private fun hasExtension(sourceSet: SourceSet, extension: String): Boolean =
        sourceSet.sourceDirectories().filter { Files.exists(it) }.any { dir ->
            Files.walk(dir).use { paths -> paths.anyMatch { it.fileName.toString().endsWith(extension) } }
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

    private fun uriToPath(uri: URI): Path? =
        runCatching { if (uri.scheme == "file") Paths.get(uri) else Paths.get(uri.schemeSpecificPart) }.getOrNull()

    private fun compileStubs(stubsByFqn: Map<String, String>) {
        val compiler = ToolProvider.getSystemJavaCompiler()
        compiler.getStandardFileManager(null, null, null).use { fm ->
            fm.setLocation(StandardLocation.CLASS_OUTPUT, listOf(stubDir.toFile()))
            val files = stubsByFqn.map { (fqn, code) -> inMemorySource(fqn, code) }
            check(compiler.getTask(null, fm, null, null, null, files).call()) { "stub compilation failed" }
        }
    }

    private fun inMemorySource(fqn: String, code: String): JavaFileObject =
        object : SimpleJavaFileObject(
            URI.create("string:///" + fqn.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE
        ) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
        }
}
