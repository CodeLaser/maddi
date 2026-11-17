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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A placeholder that is a call's ARGUMENT survives [KotlinBodyConverter.normalizeIndices]. The renumbering translates
 * the body, and MethodCall's translation removed every argument that was an empty expression -- which a placeholder
 * is. Unread code vanished from the CST and from the census with it: `ServiceLoader.load(T::class.java)` (then a
 * placeholder; now `reduce(Int::plus)` plays its part) became `load(getJava())` whenever the body needed renumbering, which every inlined scope function
 * (#88) does, and detekt's `loadExtensions` lost its verdict to the call with one argument too few.
 */
class RenumberedPlaceholderTest : KotlinScanTestBase() {

    @Test
    fun aPlaceholderArgumentIsKeptAndCounted() {
        val types = KotlinScan(runtime, sourceSet).parse("r/R.kt", """
            package r
            fun plain(): Int = listOf(1, 2).reduce(Int::plus)
            fun viaAlso(): Int = listOf(1, 2).reduce(Int::plus).also { println(it) }
            """.trimIndent() + "\n")
        val body = { name: String -> types.flatMap { it.methods() }.first { it.name() == name }.methodBody().toString() }
        assertEquals("{return CollectionsKt.reduce(CollectionsKt.listOf(1,2),k2-callable-ref-unresolved:plus);}",
            body("plain"))
        assertEquals("{int it=CollectionsKt.reduce(CollectionsKt.listOf(1,2),k2-callable-ref-unresolved:plus);{System.out.println(it);}return it;}",
            body("viaAlso"))
        assertEquals(2, PlaceholderCensus.of(types).total)
    }
}
