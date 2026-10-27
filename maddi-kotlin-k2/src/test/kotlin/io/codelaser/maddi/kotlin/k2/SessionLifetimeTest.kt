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
