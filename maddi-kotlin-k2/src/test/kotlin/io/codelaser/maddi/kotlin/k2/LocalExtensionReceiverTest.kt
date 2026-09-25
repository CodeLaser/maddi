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
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A LOCAL extension function's receiver, read implicitly inside a lambda nested in its body whose own receiver
 * shadows the name `$receiver` (`with(session) { … }`): detekt's `fun KtValueArgument.isNearestParentForSuspension()`
 * calling `getArgumentExpression()` inside `with(session)`. A receiver lambda's `$receiver` was reachable by key from
 * nested lambdas; a local function's was not.
 */
class LocalExtensionReceiverTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("lx/Lx.kt", """
            package lx
            class Session { fun resolve(e: String): Int = 1; fun String.kind(): Int = 2 }
            class Arg { fun expr(): String = "x" }
            fun outer(s: Session, a: Arg): Boolean {
                fun Arg.check(): Boolean { with(s) { return resolve(expr()) > 0 } }
                fun String.callable(): Boolean = with(s) { kind() > 0 }
                return a.check() && "y".callable()
            }
            """.trimIndent() + "\n")
    }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    /** ⛔ by identity: both receivers print as `$receiver`, so the printout cannot tell the right one from the wrong one. */
    @Test
    fun eachCallReadsTheReceiverOfItsOwnType() {
        val calls = mutableListOf<MethodCall>()
        types.first { it.simpleName() == "LxKt" }.findUniqueMethod("outer", 2).methodBody().visit { e: Element ->
            if (e is MethodCall) calls += e
            true
        }
        fun receiverType(e: io.codelaser.maddi.cst.api.expression.Expression) =
            ((e as VariableExpression).variable().parameterizedType().typeInfo()?.simpleName())
        val expr = calls.single { it.methodInfo().name() == "expr" }
        assertEquals("Arg", receiverType(expr.`object`()), "expr() is called on the local extension's receiver")
        val resolve = calls.single { it.methodInfo().name() == "resolve" }
        assertEquals("Session", receiverType(resolve.`object`()))
        val kind = calls.single { it.methodInfo().name() == "kind" }
        assertEquals("Session", receiverType(kind.`object`()), "kind is a member extension of Session")
        assertEquals("String", receiverType(kind.parameterExpressions()[0]), "its extension receiver is the local one's")
    }
}
