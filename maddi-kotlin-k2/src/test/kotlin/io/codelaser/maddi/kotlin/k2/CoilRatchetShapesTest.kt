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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A shape that raised coil's placeholder ratchet (TestCoilJvmSlice, 4 to 7): a qualified read of a property with a
 * written getter is a getter call (#82), so `this.size -= n` had a call as its compound assignment's target
 * (`LruCache.trimToSize`). The other two were coil's `EventListener.Factory.NONE`, a @JvmField `actual` companion
 * property read from common code; this fixture's expect/actual pair does not take coil's path (K2 models a getter
 * here, #96), so TestCoilJvmSlice is that shape's witness.
 */
class CoilRatchetShapesTest : KotlinScanTestBase() {

    private fun write(root: Path, fragment: String, file: String, text: String) {
        val dir = Files.createDirectories(root.resolve("src/$fragment/kotlin/p"))
        Files.writeString(dir.resolve(file), text.trimIndent() + "\n")
    }

    private fun parse(root: Path): List<TypeInfo> {
        write(root, "commonMain", "Common.kt", """
            package p
            class Cache {
                var size: Long = 0
                    get() {
                        if (field == -1L) field = 0L
                        return field
                    }
                fun trim(n: Long) {
                    this.size -= n
                    size -= n
                }
            }
            """)
        val main = SourceSetImpl.Builder().setName("main").setUri(root.toUri())
            .setSourceDirectories(listOf("commonMain").map { root.resolve("src/$it/kotlin") })
            .build()
        return KotlinProjectScan(runtime, InfoByFqn())
            .parse(listOf(main), emptyList(), Paths.get(System.getProperty("java.home")), emptyList(), listOf())
            .getValue(main)
    }

    private fun body(types: List<TypeInfo>, type: String, method: String): String =
        types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == type }
            .methods().first { it.name() == method }.methodBody().statements().joinToString(" ")

    @Test
    fun theShapes(@TempDir root: Path) {
        val types = parse(root)
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        assertEquals("this.size-=n; this.size-=n;", body(types, "Cache", "trim"))
    }
}
