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
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Context parameters (`context(session: KaSession)`, 47 declarations in detekt). kotlinc compiles them as the LEADING
 * parameters, ahead of an extension receiver; the signatures below are javap's, on kotlinc 2.4.0, for this source:
 * `top(Session, String)`, `ext2(Session, Box, int)`, `getProp(Session, Box)`, `Host.member(Session, String)`.
 */
class ContextParameterTest : KotlinScanTestBase() {

    private val source = """
        package cp

        class Box { val size: Int get() = 1 }
        class Session { fun plain(): Int = 1 }
        context(s: Session) fun top(x: String): Int = s.plain()
        context(s: Session) fun Box.ext2(y: Int): Int = size + y
        context(s: Session) val Box.prop: Int get() = size + s.plain()
        context(s: Session) fun relay(x: String): Int = top(x)

        class Host {
            context(s: Session) fun member(x: String): Int = s.plain()
            fun caller(s: Session, b: Box): Int = with(s) { member("a") + top("b") + b.ext2(1) + b.prop }
        }
        """.trimIndent() + "\n"

    private val types: List<TypeInfo> by lazy { KotlinScan(runtime, sourceSet).parse("cp/Cp.kt", source) }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == name }

    private fun signature(m: MethodInfo): String =
        m.name() + m.parameters().joinToString(",", "(", ")") { "${it.name()}:${it.parameterizedType().typeInfo()?.simpleName()}" }

    private fun calls(method: MethodInfo): List<MethodCall> {
        val calls = mutableListOf<MethodCall>()
        method.methodBody().visit { e: Element ->
            if (e is MethodCall) calls += e
            true
        }
        return calls
    }

    private fun firstArgument(call: MethodCall): ParameterInfo =
        (call.parameterExpressions()[0] as VariableExpression).variable() as ParameterInfo

    @Test
    fun contextParametersLeadTheSignature() {
        val facade = type("CpKt")
        assertEquals("top(s:Session,x:String)", signature(facade.findUniqueMethod("top", 2)))
        assertEquals("ext2(s:Session,\$receiver:Box,y:int)", signature(facade.findUniqueMethod("ext2", 3)))
        assertEquals("getProp(s:Session,\$receiver:Box)", signature(facade.findUniqueMethod("getProp", 2)))
        assertEquals("member(s:Session,x:String)", signature(type("Host").findUniqueMethod("member", 2)))
    }

    @Test
    fun theContextParameterIsAnOrdinaryParameterInTheBody() {
        val top = type("CpKt").findUniqueMethod("top", 2)
        val plain = calls(top).single { it.methodInfo().name() == "plain" }
        assertEquals(top.parameters()[0], (plain.`object`() as VariableExpression).variable())
    }

    @Test
    fun aContextArgumentFromAnEnclosingReceiver() {
        // inside `with(s) { … }` the Session in scope is the lambda's receiver, and every call passes it first
        val byName = calls(type("Host").findUniqueMethod("caller", 2)).associateBy { it.methodInfo().name() }
        listOf("member", "top", "ext2", "getProp").forEach { n ->
            val context = firstArgument(byName.getValue(n))
            assertEquals("\$receiver", context.name(), n)
            assertEquals(type("Session"), context.parameterizedType().typeInfo(), n)
        }
        assertEquals(3, byName.getValue("ext2").parameterExpressions().size, "context, receiver, value")
    }

    @Test
    fun aContextArgumentPassedOn() {
        val relay = type("CpKt").findUniqueMethod("relay", 2)
        val top = calls(relay).single { it.methodInfo().name() == "top" }
        assertEquals(relay.parameters()[0], firstArgument(top), "relay's own `s`")
    }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }
}
