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
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.variable.This
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A MEMBER extension -- `fun Box.ext()` declared inside a type -- has two receivers: the extension receiver,
 * written, and the dispatch receiver, implicit. On the JVM it is an instance method of the declaring type taking the
 * extension receiver as argument 0. detekt's whole Analysis-API surface is this shape (`expression.resolveToCall()`
 * inside `analyze(…) { }`): 246 `resolveToCall` placeholders and 50 `expressionType` accesses.
 */
class MemberExtensionTest : KotlinScanTestBase() {

    private val source = """
        package me

        class Box { val size: Int get() = 1 }
        class Session {
            fun Box.ext(): Int = size
            val Box.twice: Int get() = size + size
            fun own(x: Box): Int = x.ext() + x.twice
        }
        fun <T> analyze(s: Session, block: Session.() -> T): T = s.block()

        class K {
            fun viaCall(s: Session, x: Box): Int = analyze(s) { x.ext() }
            fun viaProperty(s: Session, x: Box): Int = analyze(s) { x.twice }
        }
        """.trimIndent() + "\n"

    private val types: List<TypeInfo> by lazy { KotlinScan(runtime, sourceSet).parse("me/Me.kt", source) }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == name }

    private fun calls(method: MethodInfo): List<MethodCall> {
        val calls = mutableListOf<MethodCall>()
        method.methodBody().visit { e: Element ->
            if (e is MethodCall) calls += e
            true
        }
        return calls
    }

    private fun assertMemberExtension(call: MethodCall, x: ParameterInfo) {
        assertEquals(type("Session"), call.methodInfo().typeInfo())
        assertEquals(1, call.methodInfo().parameters().size)
        assertTrue(call.objectIsImplicit())
        assertEquals(x, (call.parameterExpressions()[0] as VariableExpression).variable(),
            "the extension receiver is argument 0")
    }

    @Test
    fun callDispatchedOnTheReceiverOfALambda() {
        val viaCall = type("K").findUniqueMethod("viaCall", 2)
        val ext = calls(viaCall).single { it.methodInfo().name() == "ext" }
        assertMemberExtension(ext, viaCall.parameters()[1])
        val dispatch = (ext.`object`() as VariableExpression).variable() as ParameterInfo
        assertEquals("\$receiver", dispatch.name())
        assertEquals(type("Session"), dispatch.parameterizedType().typeInfo())
    }

    @Test
    fun propertyDispatchedOnTheReceiverOfALambda() {
        val viaProperty = type("K").findUniqueMethod("viaProperty", 2)
        val getter = calls(viaProperty).single { it.methodInfo().name() == "getTwice" }
        assertMemberExtension(getter, viaProperty.parameters()[1])
        assertEquals("\$receiver", ((getter.`object`() as VariableExpression).variable() as ParameterInfo).name())
    }

    @Test
    fun dispatchedOnTheClassItself() {
        val own = type("Session").findUniqueMethod("own", 1)
        val byName = calls(own).associateBy { it.methodInfo().name() }
        listOf("ext", "getTwice").forEach { n ->
            val call = byName.getValue(n)
            assertMemberExtension(call, own.parameters()[0])
            assertTrue((call.`object`() as VariableExpression).variable() is This, n)
        }
    }

    @Test
    fun noPlaceholderOutsideTheFunctionTypeInvocation() {
        val census = PlaceholderCensus.of(types)
        assertTrue(census.dumpLines().none { !it.contains("analyze(") }, census.dumpLines().joinToString("\n"))
    }
}
