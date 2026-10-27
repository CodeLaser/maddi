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

package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A Kotlin-multiplatform target's fragments (`src/commonMain/kotlin`, …, `src/jvmMain/kotlin`) in ONE source set, as
 * coil-core's JVM slice lists them. Flattened into one K2 module, an `expect` and its `actual` are rival declarations
 * and K2 resolves neither; as a dependsOn chain, K2 matches them as kotlinc does.
 */
class MultiplatformFragmentsTest : KotlinScanTestBase() {

    private fun write(root: Path, fragment: String, file: String, text: String) {
        val dir = Files.createDirectories(root.resolve("src/$fragment/kotlin/p"))
        Files.writeString(dir.resolve(file), text.trimIndent() + "\n")
    }

    private fun parse(root: Path): List<TypeInfo> {
        write(root, "commonMain", "Common.kt", """
            package p
            internal expect fun io(): Int
            expect class Box(v: Int) { fun get(): Int }
            expect class Handle
            internal expect fun Int.twice(): Int
            class User {
                fun a(): Int = io(); fun b(): Box = Box(2); fun c(x: Box): Int = x.get()
                fun d(h: Handle): Handle = h; fun e(i: Int): Int = i.twice()
            }
            """)
        write(root, "nonJsCommonMain", "Io.nonJsCommon.kt", """
            package p
            internal actual fun io(): Int = 1
            internal actual fun Int.twice(): Int = this * 2
            actual typealias Handle = java.lang.StringBuilder
            """)
        write(root, "jvmMain", "Box.jvm.kt", """
            package p
            actual class Box actual constructor(private val v: Int) { actual fun get(): Int = v }
            """)
        val main = SourceSetImpl.Builder().setName("main").setUri(root.toUri())
            .setSourceDirectories(listOf("commonMain", "nonJsCommonMain", "jvmMain").map { root.resolve("src/$it/kotlin") })
            .build()
        return KotlinProjectScan(runtime, InfoByFqn())
            .parse(listOf(main), emptyList(), Paths.get(System.getProperty("java.home")), emptyList(), listOf())
            .getValue(main)
    }

    private fun all(types: List<TypeInfo>) = types.flatMap { it.recursiveSubTypeStream().toList() }

    @Test
    fun expectMatchedToActual(@TempDir root: Path) {
        val types = parse(root)
        val user = all(types).single { it.simpleName() == "User" }
        // the `expect` fun's call goes to the actual's facade; the `expect` class is dropped, the actual holds the name
        assertEquals("""
            a: return Io_nonJsCommonKt.io();
            b: return new Box(2);
            c: return x.get();
            d: return h;
            e: return Io_nonJsCommonKt.twice(i);
            p.Box p.Io_nonJsCommonKt p.User
            """.trimIndent(), user.methods().sortedBy { it.name() }.joinToString("\n") { it.name() + ": " + it.methodBody().statements().joinToString(" ") } + "\n" +
            all(types).map { it.fullyQualifiedName() }.sorted().joinToString(" "))
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    /** The `actual typealias` expands to the JVM class: no shell named `p.Handle`. */
    @Test
    fun anActualTypealiasIsItsExpansion(@TempDir root: Path) {
        val user = all(parse(root)).single { it.simpleName() == "User" }
        assertEquals("java.lang.StringBuilder", user.methods().single { it.name() == "d" }.returnType().typeInfo()?.fullyQualifiedName())
    }

    @Test
    fun onlyFragmentsAreSplit(@TempDir root: Path) {
        assertEquals(3, multiplatformFragments(listOf("commonMain", "nonJsCommonMain", "jvmMain")
            .map { root.resolve("src/$it/kotlin") })?.size)
        assertNull(multiplatformFragments(listOf(root.resolve("src/main/kotlin"), root.resolve("src/main/generated"))))
        assertNull(multiplatformFragments(listOf(root.resolve("src/jvmMain/kotlin"), root.resolve("src/commonMain/kotlin"))))
    }

    /**
     * A `@JvmField actual` companion property read from common code: K2 resolves the read to the `expect` property,
     * which has no @JvmField, but kotlinc compiles the actual to a static field of the enclosing interface, with no
     * getter. maddi#96 reported a `Companion.getNONE()` here: its fixture had no kotlin-stdlib on the library path, so
     * `@JvmField` itself was unresolved and the `actual` read as a plain property. With the stdlib, as every real
     * build has it, the read is the field.
     */
    @Test
    fun jvmFieldActualCompanionPropertyIsAField(@TempDir root: Path) {
        write(root, "commonMain", "Listener.kt", """
            package p
            expect abstract class Listener {
                fun interface Factory {
                    fun create(): Listener
                    companion object { val NONE: Factory }
                }
            }
            class User { fun none(f: Listener.Factory?): Listener.Factory = f ?: Listener.Factory.NONE }
            """)
        write(root, "jvmMain", "Listener.jvm.kt", """
            package p
            actual abstract class Listener {
                actual fun interface Factory {
                    actual fun create(): Listener
                    actual companion object { @JvmField actual val NONE = Factory { object : Listener() {} } }
                }
            }
            """)
        val main = SourceSetImpl.Builder().setName("main").setUri(root.toUri())
            .setSourceDirectories(listOf("commonMain", "jvmMain").map { root.resolve("src/$it/kotlin") })
            .build()
        // kotlin.jvm.JvmField is in kotlin-stdlib: without it on the library path, the annotation is unresolved
        val stdlib = Paths.get(JvmField::class.java.protectionDomain.codeSource.location.toURI())
        val types = KotlinProjectScan(runtime, InfoByFqn())
            .parse(listOf(main), listOf(stdlib), Paths.get(System.getProperty("java.home")), emptyList(), listOf())
            .getValue(main)
        val user = all(types).single { it.simpleName() == "User" }
        assertEquals("return f==null?Factory.NONE:f;",
            user.findUniqueMethod("none", 1).methodBody().statements().joinToString(" "))
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }
}
