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
