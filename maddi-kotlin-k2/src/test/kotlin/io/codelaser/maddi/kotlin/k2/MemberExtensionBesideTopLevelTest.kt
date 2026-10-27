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
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * A MEMBER extension whose file also has a facade: Exposed's `Expression.kt` declares `QueryBuilder` with a member
 * `fun <T> Iterable<T>.appendTo(...)` AND a top-level `fun <T> Iterable<T>.appendTo(builder, ...)`, so the file has
 * a `ExpressionKt` facade. Inside a `QueryBuilder` receiver lambda, K2 resolves `columns.appendTo(...)` to the
 * MEMBER; the call was routed to the facade by the declaring FILE -- with an omitted argument, the member's
 * `$default` became a static call on `ExpressionKt` (the CST constructor's assertion: a type expression as the
 * object of an instance method, which stopped Exposed's whole parse in AbstractQuery). The dispatch receiver is the
 * lambda's `QB`, as for any member extension (see [MemberExtensionTest]).
 */
class MemberExtensionBesideTopLevelTest : KotlinScanTestBase() {

    private val source = """
        package mx

        class QB {
            fun <T> Iterable<T>.appendTo(prefix: String = "", postfix: String = ""): QB = this@QB
        }

        fun <T> Iterable<T>.appendTo(builder: QB, prefix: String = ""): QB = builder

        fun <T> build(b: QB, block: QB.() -> T): T = b.block()

        class K {
            fun omitted(b: QB, xs: List<Int>): QB = build(b) { xs.appendTo(prefix = "(") }
            fun allWritten(b: QB, xs: List<Int>): QB = build(b) { xs.appendTo("(", ")") }
        }
        """.trimIndent() + "\n"

    private val types: List<TypeInfo> by lazy { KotlinScan(runtime, sourceSet).parse("mx/Mx.kt", source) }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == name }

    private fun appendTo(method: MethodInfo): MethodCall {
        val calls = mutableListOf<MethodCall>()
        method.methodBody().visit { e: Element ->
            if (e is MethodCall && e.methodInfo().name().startsWith("appendTo")) calls += e
            true
        }
        return calls.single()
    }

    private fun assertMemberCall(call: MethodCall, xs: Any, name: String) {
        assertEquals(type("QB"), call.methodInfo().typeInfo(), "the member, not the facade's top-level overload")
        assertEquals(name, call.methodInfo().name())
        assertFalse(call.methodInfo().isStatic)
        assertEquals(xs, (call.parameterExpressions()[0] as VariableExpression).variable(),
            "the extension receiver is argument 0")
    }

    @Test
    fun anOmittedArgumentCallsTheMembersDefault() {
        val omitted = type("K").findUniqueMethod("omitted", 2)
        assertMemberCall(appendTo(omitted), omitted.parameters()[1], "appendTo\$default")
    }

    @Test
    fun allArgumentsWrittenCallsTheMemberItself() {
        val allWritten = type("K").findUniqueMethod("allWritten", 2)
        assertMemberCall(appendTo(allWritten), allWritten.parameters()[1], "appendTo")
    }
}
