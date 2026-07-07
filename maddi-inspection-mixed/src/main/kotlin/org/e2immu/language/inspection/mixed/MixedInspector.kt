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

import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.inspection.api.integration.JavaInspector
import org.e2immu.language.inspection.api.resource.CompiledTypesManager
import org.e2immu.language.inspection.kotlin.JavaStubGenerator
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl
import org.e2immu.language.inspection.resource.InfoByFqn
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import java.net.URI
import java.nio.file.Files
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider

/**
 * Parses a mixed Java + Kotlin module over **one shared core**: the openjdk (javac) and K2 front-ends share
 * one [Runtime], one [InfoByFqn] and one [CompiledTypesManager], so every `java.*`/classpath type and every
 * cross-language *source* type is a single `TypeInfo` instance. See `mixed-language-integration.md` (in
 * `maddi-inspection-kotlin`) for the design and phases.
 *
 * The openjdk inspector owns the shared state; the Kotlin front-end borrows it. The order is **Kotlin-first**:
 *
 * 1. Parse the Kotlin sources (K2 also reads the Java sources, so Kotlin resolves Java-source references).
 * 2. Generate a signature-only Java stub for each Kotlin type and compile it onto the module's classpath.
 * 3. Parse the Java sources; a Java reference to a Kotlin type resolves against the stub, and — because the
 *    openjdk scan checks the shared registry first — reuses the shared Kotlin `TypeInfo`.
 *
 * Both languages share one instance for library/JDK types and for **Java→Kotlin** source references. A
 * **Kotlin→Java** reference to a source type *in the same batch* resolves against K2's own view (a provisional
 * type), not the openjdk-built one — making that share too needs a skeleton pre-pass (future work). Kotlin
 * referencing an already-compiled or library Java type shares fine.
 *
 * Single-threaded (javac). The running JVM needs the openjdk `--add-exports jdk.compiler/com.sun.tools.javac.*`.
 */
class MixedInspector {

    private val stubDir = Files.createTempDirectory("mixed-stubs")

    /** The single shared source set both front-ends populate (its classpath includes the generated stubs). */
    val sourceSet = SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL).setUri(URI.create("file:/"))
        .setDependencies(
            listOf(
                SourceSetImpl.javaBase(),
                SourceSetImpl.Builder().setName("mixed-stubs").setUri(stubDir.toUri()).setExternalLibrary(true).build()
            )
        ).build()

    private val javaInspector = JavaInspectorImpl().apply {
        initialize(
            InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath("jmod:java.base")
                .addClassPathParts(sourceSet.dependencies().last()) // the stub directory
                .build()
        )
        sourceSet.computePriorityDependencies()
        // a warmup scan so the manager's lazy loader has a live javac task from the start — otherwise the FIRST
        // (Kotlin) parse cannot delegate java.* to the shared core and would build a divergent K2 view.
        onlyPreload()
    }

    val runtime: Runtime get() = javaInspector.runtime()
    val infoByFqn: InfoByFqn get() = javaInspector.infoByFqn()
    val compiledTypesManager: CompiledTypesManager get() = javaInspector.compiledTypesManager()

    /** [kotlinTypes] and [javaTypes] are the primary types of each language, cross-referencing shared instances. */
    data class Result(val kotlinTypes: List<TypeInfo>, val javaTypes: List<TypeInfo>)

    /**
     * @param kotlinSourcesByFileName e.g. `"K.kt" -> "package a.b; class K …"`
     * @param javaSourcesByFqn        e.g. `"a.b.UseK" -> "package a.b; class UseK …"`
     */
    fun parse(kotlinSourcesByFileName: Map<String, String>, javaSourcesByFqn: Map<String, String>): Result {
        // 1) Kotlin first; K2 also reads the Java sources so Kotlin can resolve Java-source references.
        val javaForK2 = javaSourcesByFqn.mapKeys { (fqn, _) -> fqn.replace('.', '/') + ".java" }
        val kotlinTypes = KotlinScan(runtime, sourceSet, infoByFqn, compiledTypesManager)
            .parse(kotlinSourcesByFileName, javaForK2)

        // 2) generate + compile a stub for each Kotlin type onto the classpath so javac can resolve it
        if (kotlinTypes.isNotEmpty()) {
            compileStubs(kotlinTypes.associate { it.fullyQualifiedName() to JavaStubGenerator.stub(it) })
        }

        // 3) Java. A reference to a Kotlin type resolves via the stub and reuses the shared Kotlin TypeInfo.
        val javaTypes = javaSourcesByFqn.map { (fqn, src) -> javaInspector.parse(fqn, src) }
        return Result(kotlinTypes, javaTypes)
    }

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
