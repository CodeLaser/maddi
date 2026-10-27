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
