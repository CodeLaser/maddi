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
package io.codelaser.maddi.kotlin.api

import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.resource.InfoByFqn
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.net.URLClassLoader

/**
 * ⛔⛔ <b>THE CONTRACT SAID "NEVER FALLS BACK TO THE CLASSPATH", AND `get()` FELL BACK TO THE CLASSPATH.</b>
 *
 * [KotlinFrontEnds]' own doc: *"a front end that quietly fell back to the classpath would reintroduce the leak
 * this boundary exists to close — silently, which is the failure mode of the original defect."* Its `get()`
 * then did exactly that: `instance ?: KotlinFrontEnd.load()`, against the host's own loader. A host that
 * forgot `K2Realm.installIfAbsent()` and had `maddi-kotlin-k2` on its classpath — transitively, by accident,
 * the way G46 began — got the flat compiler with all its shadowing, and nothing said so.
 *
 * So the flat choice stays AVAILABLE and becomes EXPLICIT: `KotlinFrontEnds.install(KotlinFrontEnd.load())`,
 * one line a test suite can own. What goes is the implicit form.
 *
 * The test puts a provider on this module's classpath ([FlatFakeFrontEnd], registered in test resources)
 * precisely so the fallback has something to find: without one, "no fallback" and "fallback found nothing"
 * would look the same.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class NoSilentFallbackTest {

    @Test
    @Order(1)
    fun nothingInstalledIsARefusalEvenWithAFrontEndOnTheClasspath() {
        val e = assertThrows(IllegalStateException::class.java) { KotlinFrontEnds.get() }

        val message = e.message ?: ""
        assertTrue(message.contains("maddi.k2.classpath"),
            "the refusal must say how a host installs the realm: $message")
        assertTrue(message.contains("KotlinFrontEnds.install"),
            "…and how a host that WANTS the flat classpath says so: $message")
    }

    /** the second message of the pair was "visible to ." — the loader it meant to name was never interpolated */
    @Test
    @Order(2)
    fun aLoaderThatSeesNoFrontEndIsNamed() {
        val empty = URLClassLoader("nothing-here", arrayOf(), null)

        val e = assertThrows(IllegalStateException::class.java) { KotlinFrontEnd.load(empty) }

        assertTrue(e.message!!.contains("nothing-here"), "the refusal must name the loader: ${e.message}")
    }

    @Test
    @Order(3)
    fun theFlatChoiceIsStillOneExplicitLine() {
        KotlinFrontEnds.install(KotlinFrontEnd.load())

        assertTrue(KotlinFrontEnds.get() is FlatFakeFrontEnd)
        assertSame(KotlinFrontEnds.get(), KotlinFrontEnds.get(), "installed once, answered every time")
    }
}

/** Test-only. A provider for the fallback to find; every call fails, because no test may parse with it. */
class FlatFakeFrontEnd : KotlinFrontEnd {
    override fun sourceScan(runtime: Runtime, sourceSet: SourceSet, infoByFqn: InfoByFqn,
                            compiledTypesManager: CompiledTypesManager?): KotlinSourceScan = error("fake")

    override fun projectScan(runtime: Runtime, infoByFqn: InfoByFqn,
                             compiledTypesManager: CompiledTypesManager?): KotlinProjectScanner = error("fake")

    override fun referenceIndex(): KotlinReferenceIndex = error("fake")

    override fun referenceRecall(samplesPerCell: Int): KotlinReferenceRecall = error("fake")
}
