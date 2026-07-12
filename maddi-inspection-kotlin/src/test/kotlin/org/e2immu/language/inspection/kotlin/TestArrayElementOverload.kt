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
 * A Kotlin `Array<T>` is the JVM reified array `T[]`, so `Array<Double>` and `Array<Float>` are distinct
 * JVM parameter types (`Double[]` vs `Float[]`) — two functions differing only in the array's element type are
 * valid overloads and must NOT collapse to one `kotlin.Array` signature (they did before, colliding as "Two
 * methods with the same FQN"; mirrors kotlin-stdlib's `Array<Double>.max` vs `Array<Float>.max`).
 */
class TestArrayElementOverload {

    @Test
    fun arrayElementTypeDistinguishesOverloads() {
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()
        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)

        val types = inspector.parse(
            sourceSet, mapOf(
                "A.kt" to """
                    package p
                    fun f(a: Array<Double>): Int = a.size
                    fun f(a: Array<Float>): Int = a.size
                """.trimIndent()
            )
        )

        val facade = types.first { it.simpleName() == "AKt" }
        // both overloads survive (distinct erased params Double[] / Float[]); .size still resolves in the bodies
        assertEquals(2, facade.methods().count { it.name() == "f" })
    }
}
