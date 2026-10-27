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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A member read through a CAPTURED type: detekt's `resolveToCall()?.singleFunctionCallOrNull()?.symbol?.callableId`,
 * where `symbol` is typed by the capture of a star projection. Java types such an expression by the wildcard's
 * bound; the mapper sent it to Object, where `getCallableId` does not exist.
 */
class CapturedTypeTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ct/Ct.kt", """
            package ct
            abstract class Base { abstract val id: Int }
            class Holder<T : Base>(val t: T)
            fun find(): Holder<*>? = null
            class K {
                fun star(h: Holder<*>): Int = h.t.id
                fun out(h: Holder<out Base>): Int = h.t.id
                fun chained(): Int? = find()?.t?.id
            }
            """.trimIndent() + "\n")
    }

    private fun getIdIn(method: String, arity: Int = 1): MethodCall {
        val calls = mutableListOf<MethodCall>()
        types.first { it.simpleName() == "K" }.findUniqueMethod(method, arity).methodBody().visit { e: Element ->
            if (e is MethodCall && e.methodInfo().name() == "getId") calls += e
            true
        }
        return calls.single()
    }

    @Test
    fun aStarProjectionIsReadThroughItsBound() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        assertEquals("Base", getIdIn("star").methodInfo().typeInfo().simpleName())
    }

    @Test
    fun aSafeCallChainOverAStarProjection() {
        // detekt's shape: `resolveToCall()?.singleFunctionCallOrNull()?.symbol?.callableId`
        assertEquals("Base", getIdIn("chained", 0).methodInfo().typeInfo().simpleName())
    }

    @Test
    fun anOutProjectionIsReadThroughItsType() {
        assertEquals("Base", getIdIn("out").methodInfo().typeInfo().simpleName())
    }
}
