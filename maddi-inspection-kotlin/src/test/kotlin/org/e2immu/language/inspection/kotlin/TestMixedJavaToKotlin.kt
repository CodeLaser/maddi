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

import org.e2immu.language.inspection.api.integration.JavaInspector
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider

/**
 * Mixed-language integration, Phase 3/4 end-to-end (Java → Kotlin). The two front-ends share one `Runtime`,
 * one `InfoByFqn` and one `CompiledTypesManager`. The Kotlin front-end registers `a.b.K`; a signature-only
 * Java stub of `K` is compiled onto the openjdk source set's classpath so javac can resolve it; the openjdk
 * front-end then parses a Java source that references `K` and — because it checks the shared registry before
 * loading the stub — **reuses the Kotlin `K`**. So a Kotlin source type is one shared instance across both
 * parsers, in the direction that javac cannot read natively.
 */
class TestMixedJavaToKotlin {

    @Test
    fun javaResolvesAKotlinSourceType() {
        val stubDir = Files.createTempDirectory("k2-stubs")
        // the stub directory is a classpath dependency of the (single, shared) 'main' source set
        val stubs = SourceSetImpl.Builder().setName("stubs").setUri(stubDir.toUri()).setExternalLibrary(true).build()
        val main = SourceSetImpl.Builder().setName(JavaInspector.TEST_PROTOCOL).setUri(URI.create("file:/"))
            .setDependencies(listOf(SourceSetImpl.javaBase(), stubs)).build()

        // the openjdk front-end owns the shared Runtime + InfoByFqn + CompiledTypesManager
        val javaInspector = JavaInspectorImpl()
        javaInspector.initialize(
            InputConfigurationImpl.Builder()
                .addSourceSets(main)
                .addClassPath("jmod:java.base")
                .addClassPathParts(stubs)
                .build()
        )
        main.computePriorityDependencies()

        val runtime = javaInspector.runtime()
        val infoByFqn = javaInspector.infoByFqn()
        val ctm = javaInspector.compiledTypesManager()

        // 1) the Kotlin front-end — sharing runtime + registry + manager — registers a.b.K into 'main'
        val kotlinK = KotlinScan(runtime, main, infoByFqn, ctm).parse(
            "K.kt", "package a.b\nclass K(val id: Int) { fun label(): String = \"k\" }\n"
        ).first { it.simpleName() == "K" }

        // 2) generate + compile K's signature-only stub onto the classpath dir, so javac can resolve `K`
        compileToDir(mapOf("a.b.K" to JavaStubGenerator.stub(kotlinK)), stubDir)

        // 3) the openjdk front-end parses a Java source that references the Kotlin K
        val useK = javaInspector.parse(
            "a.b.UseK",
            "package a.b;\npublic class UseK {\n  public K field;\n  public String go(K k) { return k.label(); }\n}\n"
        )

        // 4) the Java field's type is the SAME shared Kotlin K TypeInfo (registry reuse, not the stub)
        val fieldType = useK.getFieldByName("field", true).type().typeInfo()
        assertSame(kotlinK, fieldType, "a.b.K must be ONE shared TypeInfo across the Kotlin and Java front-ends")
    }

    /** Compile the in-memory Java sources to `.class` files under [outDir] (populating the stub classpath). */
    private fun compileToDir(sourcesByFqn: Map<String, String>, outDir: Path) {
        val compiler = ToolProvider.getSystemJavaCompiler()
        compiler.getStandardFileManager(null, null, null).use { fm ->
            fm.setLocation(StandardLocation.CLASS_OUTPUT, listOf(outDir.toFile()))
            val files = sourcesByFqn.map { (fqn, code) -> inMemorySource(fqn, code) }
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
