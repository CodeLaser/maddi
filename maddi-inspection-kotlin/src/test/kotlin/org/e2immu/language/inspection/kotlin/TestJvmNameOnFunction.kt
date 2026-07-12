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

import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * `@JvmName` on a function disambiguates two overloads that erase to the same JVM signature (here both take a
 * `Function1` after erasure); without honouring it the two collide as "Two methods with the same FQN". Mirrors
 * kotlin-stdlib's `flatMap((T)->Iterable)` vs `flatMap((T)->Sequence) @JvmName("flatMapSequence")`.
 */
class TestJvmNameOnFunction {

    @Test
    fun jvmNameDisambiguatesOverloads() {
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()
        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)

        val types = inspector.parse(
            sourceSet, mapOf(
                "A.kt" to """
                    package p
                    fun f(g: (Int) -> Iterable<Int>): Int = 0
                    @kotlin.jvm.JvmName("fSeq")
                    fun f(g: (Int) -> Sequence<Int>): Int = 1
                """.trimIndent()
            )
        )

        val facade = types.first { it.simpleName() == "AKt" }
        assertEquals(setOf("f", "fSeq"), facade.methods().map { it.name() }.toSet())
    }
}
