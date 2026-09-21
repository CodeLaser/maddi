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
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The K2 session does not outlive the parse: once [KotlinScan.parse] or [KotlinProjectScan.parse] returns, its PSI
 * is unreachable, while the CST it produced stays usable.
 *
 * An undisposed session stays reachable through IntelliJ's static Disposer tree. A host that parses more than once,
 * as the refactoring server does after every write, then kept every session it had ever built: 1,413 live `KtFile`s
 * more per detekt parse.
 */
class SessionLifetimeTest : KotlinScanTestBase() {

    private val source = """
        package p

        class A {
            fun f(): Int = g() + 1
            fun g(): Int = 41
        }
        """.trimIndent()

    /** Holds the first file of the parse, weakly. */
    private class Witness : K2ParseObserver() {
        var file: WeakReference<KtFile>? = null

        override fun observe(runtime: Runtime, ktFiles: List<KtFile>, types: List<TypeInfo>,
                             sourceSetName: (KtFile) -> String) {
            file = WeakReference(ktFiles.first())
        }
    }

    private fun cleared(reference: WeakReference<*>): Boolean {
        repeat(40) {
            System.gc()
            if (reference.get() == null) return true
            Thread.sleep(50)
        }
        return false
    }

    private fun assertTheCstOutlivesTheSession(types: List<TypeInfo>) {
        val a = types.single { it.fullyQualifiedName() == "p.A" }
        assertEquals(listOf("f", "g"), a.methods().map { it.name() }.sorted())
    }

    @Test
    fun anInMemoryParseReleasesItsSession() {
        val witness = Witness()
        val types = KotlinScan(runtime, sourceSet).parse(mapOf("p/A.kt" to source), emptyMap(), listOf(witness))
        assertTheCstOutlivesTheSession(types)
        assertTrue(cleared(witness.file!!), "the parse's KtFile is still reachable after the parse returned")
    }

    @Test
    fun aProjectParseReleasesItsSession(@TempDir root: Path) {
        val dir = Files.createDirectories(root.resolve("src/main/kotlin/p"))
        Files.writeString(dir.resolve("A.kt"), source)
        val main = SourceSetImpl.Builder().setName("main").setUri(root.toUri())
            .setSourceDirectories(listOf(root.resolve("src/main/kotlin"))).build()
        val witness = Witness()
        val types = KotlinProjectScan(runtime, InfoByFqn())
            .parse(listOf(main), emptyList(), Paths.get(System.getProperty("java.home")), emptyList(), listOf(witness))
            .getValue(main)
        assertTheCstOutlivesTheSession(types)
        assertTrue(cleared(witness.file!!), "the parse's KtFile is still reachable after the parse returned")
    }
}
