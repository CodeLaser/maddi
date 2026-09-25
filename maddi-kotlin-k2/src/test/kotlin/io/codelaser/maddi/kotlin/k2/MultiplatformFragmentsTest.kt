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
}
