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
 * Flow: the openjdk [JavaInspectorImpl] owns the shared `Runtime`/`InfoByFqn`/`CompiledTypesManager` and is
 * initialised with the Java source sets (+ the generated-stub directory on their classpath). Kotlin runs first
 * via [KotlinProjectScan] (one `KaSourceModule` per source set, dependency order, sharing the core); a stub is
 * generated and compiled for every Kotlin type; then the Java sets parse from disk, resolving Kotlin references
 * against the stubs and reusing the shared Kotlin `TypeInfo`.
 *
 * Current scope: a Java set's Kotlin-set dependencies are satisfied via the stubs (so they are dropped from the
 * Java inspector's view); Java→Java dependencies across withDependencies-rebuilt sets, and the Kotlin→Java
 * source direction, are follow-ups. Single-threaded (javac); needs the openjdk `--add-exports`.
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

        val stubSet: SourceSet = SourceSetImpl.Builder().setName("mixed-stubs")
            .setUri(stubDir.toUri()).setExternalLibrary(true).build()

        // the Java front-end owns the shared core; a Java set's Kotlin-set deps are satisfied by the stubs, so
        // only its library + Java-set deps are kept, plus the stub directory.
        val javaSetIdentity = javaSets.toSet()
        val javaInspector = JavaInspectorImpl()
        val javaConfig = InputConfigurationImpl.Builder().addClassPath("jmod:java.base").addClassPathParts(stubSet)
        javaSets.forEach { js ->
            val keptDeps = js.dependencies().filter { it.externalLibrary() || it in javaSetIdentity }
            javaConfig.addSourceSets(js.withDependencies(keptDeps + stubSet))
        }
        javaInspector.initialize(javaConfig.build())
        javaInspector.onlyPreload() // warm the CTM lazy loader before Kotlin delegates java.* to it

        val runtime = javaInspector.runtime()
        val infoByFqn = javaInspector.infoByFqn()
        val ctm = javaInspector.compiledTypesManager()

        // Kotlin: multi-source-set, dependency order, sharing the core
        val libraryRoots = config.classPathParts()
            .filter { it.externalLibrary() && !it.partOfJdk() }
            .mapNotNull { uriToPath(it.uri()) }.filter { Files.exists(it) }
        val jdkHome = Paths.get(System.getProperty("java.home"))
        val kotlinBySourceSet = KotlinProjectScan(runtime, infoByFqn, ctm)
            .parse(dependencyOrder(kotlinSets), libraryRoots, jdkHome)

        // a stub per Kotlin type so javac resolves Kotlin references (reusing the shared TypeInfo)
        val kotlinTypes = kotlinBySourceSet.values.flatten()
        if (kotlinTypes.isNotEmpty()) {
            compileStubs(kotlinTypes.associate { it.fullyQualifiedName() to JavaStubGenerator.stub(it) })
        }

        // Java: parse from disk (empty map => read the configured source directories)
        val summary = javaInspector.parse(mapOf(), JavaInspector.ParseOptions.Builder().build())
        return Result(kotlinBySourceSet, summary.parseResult().primaryTypes().toList())
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
