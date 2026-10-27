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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
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
