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
