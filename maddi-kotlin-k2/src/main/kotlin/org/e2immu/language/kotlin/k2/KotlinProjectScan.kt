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

package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.inspection.api.resource.CompiledTypesManager
import org.e2immu.language.inspection.resource.InfoByFqn
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * Parses a whole Kotlin project — several [SourceSet]s with real source **directories** on disk, over a real
 * library **classpath** — in ONE standalone K2 session, then drives [KotlinScan] per source set. This is the
 * multi-module analogue of [KotlinScan.parse] (which builds a single-module session from in-memory files over
 * the process classpath); the driver ([org.e2immu.language.inspection.kotlin.KotlinInspector]) lives one module
 * up and translates an `InputConfiguration` into the plain inputs here (it cannot see the K2 Analysis API).
 *
 * The session has one `KtSourceModule` per source set, each wired to the JDK, the library classpath, and its
 * upstream source-set modules. Conversion runs in the given (dependency) order and shares one [InfoByFqn], so a
 * type in a dependent set resolves to the very same [TypeInfo] produced for its upstream set.
 */
class KotlinProjectScan(
    private val runtime: Runtime,
    private val infoByFqn: InfoByFqn,
    // Mixed-language: the Java front-end's CompiledTypesManager, so `java.*`/classpath types resolve to ONE
    // shared bytecode-authoritative TypeInfo. Null = standalone Kotlin (K2 loads library types itself).
    private val compiledTypesManager: CompiledTypesManager? = null,
) {

    /**
     * @param orderedSourceSets source sets in dependency order (a set's dependencies appear before it). Each
     *        must carry its `sourceDirectories()` and (for cross-set links) its `dependencies()`.
     * @param libraryRoots      jars/class-dirs for the library classpath (kotlin-stdlib, third-party, …).
     * @param jdkHome           the JDK to resolve `java.*`/`kotlin`-mapped types against.
     * @param javaSourceRoots   directories of Java **source** the Kotlin sets reference (mixed-language,
     *        Kotlin→Java). K2 resolves the symbols from these (its `addSourceRoot` includes `.java`); the CST
     *        `TypeInfo` still comes from the shared registry/CTM (those Java sets must be parsed first).
     */
    fun parse(orderedSourceSets: List<SourceSet>, libraryRoots: List<Path>, jdkHome: Path,
              javaSourceRoots: List<Path> = emptyList()): Map<SourceSet, List<TypeInfo>> {
        val jvm = JvmPlatforms.defaultJvmPlatform
        val moduleBySourceSet = LinkedHashMap<SourceSet, KaSourceModule>()

        val session = buildStandaloneAnalysisAPISession {
            buildKtModuleProvider {
                platform = jvm
                val jdk = buildKtSdkModule {
                    platform = jvm
                    addBinaryRootsFromJdkHome(jdkHome, isJre = false)
                    libraryName = "jdk"
                }
                addModule(jdk)
                val library = buildKtLibraryModule {
                    platform = jvm
                    addBinaryRoots(libraryRoots)
                    libraryName = "classpath"
                }
                addModule(library)
                // a single module holding the referenced Java source, so every Kotlin module can resolve those
                // types (without laying the .java files into each Kotlin module's own roots)
                val javaExisting = javaSourceRoots.filter { Files.exists(it) }
                val javaModule = if (javaExisting.isEmpty()) null else buildKtSourceModule {
                    moduleName = "java-sources"
                    platform = jvm
                    javaExisting.forEach { addSourceRoot(it) }
                    addRegularDependency(jdk)
                    addRegularDependency(library)
                }
                javaModule?.let { addModule(it) }
                // dependency order => a dependent finds its already-built upstream module in the map
                orderedSourceSets.forEach { ss ->
                    val module = buildKtSourceModule {
                        moduleName = ss.name()
                        platform = jvm
                        ss.sourceDirectories().filter { Files.exists(it) }.forEach { addSourceRoot(it) }
                        addRegularDependency(jdk)
                        addRegularDependency(library)
                        javaModule?.let { addRegularDependency(it) }
                        ss.dependencies().forEach { dep -> moduleBySourceSet[dep]?.let { addRegularDependency(it) } }
                    }
                    addModule(module)
                    moduleBySourceSet[ss] = module
                }
            }
        }

        val result = LinkedHashMap<SourceSet, List<TypeInfo>>()
        orderedSourceSets.forEach { ss ->
            val module = moduleBySourceSet[ss]!!
            val ktFiles = (session.modulesWithFiles[module] ?: emptyList()).filterIsInstance<KtFile>()
            result[ss] = KotlinScan(runtime, ss, infoByFqn, compiledTypesManager).convert(ktFiles)
        }
        return result
    }
}
