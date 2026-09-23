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
import io.codelaser.maddi.cst.api.variable.This
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A member called or read on an IMPLICIT receiver that is not the class's own `this`: the receiver of the extension
 * function the code is written in, or of a lambda with receiver. The name-based lookup looked in the enclosing class
 * only; on detekt these were ~1,100 of 3,434 placeholders.
 */
class ImplicitReceiverTest : KotlinScanTestBase() {

    private val source = """
        package ir

        class Md { fun append(s: String) {} }
        fun Md.h1(t: String) = append("# " + t)

        class Box { val size: Int get() = 1 }
        fun Box.twice(): Int = size * 2

        class Session { fun plain(): Int = 1 }
        fun <T> analyze(s: Session, block: Session.() -> T): T = s.block()

        open class Shape
        class Circle : Shape() { val r: Int get() = 1 }
        fun Shape.radius(): Int = if (this is Circle) r else 0

        class K {
            fun own(): Int = 2
            fun nested(b: Box, s: Session): Int = with(b) { with(s) { size } }
            fun viaLambda(s: Session): Int = analyze(s) { plain() }
            fun viaWith(b: Box): Int = with(b) { size }
            fun control(): Int = own()
        }
        """.trimIndent() + "\n"

    private val types: List<TypeInfo> by lazy { KotlinScan(runtime, sourceSet).parse("ir/Ir.kt", source) }

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

    private fun receiverParameter(call: MethodCall): ParameterInfo =
        (call.`object`() as VariableExpression).variable() as ParameterInfo

    @Test
    fun callOnTheExtensionReceiver() {
        val h1 = type("IrKt").findUniqueMethod("h1", 2)
        val append = calls(h1).single { it.methodInfo().name() == "append" }
        assertEquals(type("Md"), append.methodInfo().typeInfo())
        assertEquals(h1.parameters()[0], receiverParameter(append), "`append` is `\$receiver.append`")
        assertTrue(append.objectIsImplicit())
    }

    @Test
    fun computedPropertyOfTheExtensionReceiver() {
        // `size` has no backing field: on the JVM it is `$receiver.getSize()`
        val twice = type("IrKt").findUniqueMethod("twice", 1)
        val getSize = calls(twice).single { it.methodInfo().name() == "getSize" }
        assertEquals(type("Box"), getSize.methodInfo().typeInfo())
        assertEquals(twice.parameters()[0], receiverParameter(getSize))
    }

    @Test
    fun computedPropertyOfTheReceiverOfALambda() {
        // detekt's `with(configSpec) { configPaths }`
        val getSize = calls(type("K").findUniqueMethod("viaWith", 1)).single { it.methodInfo().name() == "getSize" }
        assertEquals(type("Box"), getSize.methodInfo().typeInfo())
        val receiver = receiverParameter(getSize)
        assertEquals("\$receiver", receiver.name())
        assertEquals(type("Box"), receiver.parameterizedType().typeInfo())
    }

    @Test
    fun outerLambdaReceiverInsideAnInnerOne() {
        // `size` is the OUTER `with(b)`'s receiver; the innermost `$receiver` in scope is the Session
        val nested = type("K").findUniqueMethod("nested", 2)
        val getSize = calls(nested).single { it.methodInfo().name() == "getSize" }
        val receiver = receiverParameter(getSize)
        assertEquals(type("Box"), receiver.parameterizedType().typeInfo())
        assertEquals("\$receiver", receiver.name())
    }

    @Test
    fun memberOfASmartCastReceiver() {
        // `r` is Circle's, on the extension receiver declared as Shape and narrowed by `this is Circle`
        val radius = type("IrKt").findUniqueMethod("radius", 1)
        val getR = calls(radius).single { it.methodInfo().name() == "getR" }
        assertEquals(type("Circle"), getR.methodInfo().typeInfo())
        assertEquals(radius.parameters()[0], receiverParameter(getR))
    }

    /** Already converted before implicit receivers were asked of K2 (the lambda's `$receiver` is in scope); a guard. */
    @Test
    fun callOnTheReceiverOfALambda() {
        val plain = calls(type("K").findUniqueMethod("viaLambda", 1)).single { it.methodInfo().name() == "plain" }
        assertEquals(type("Session"), plain.methodInfo().typeInfo())
        val receiver = receiverParameter(plain)
        assertEquals("\$receiver", receiver.name())
        assertEquals(type("Session"), receiver.parameterizedType().typeInfo())
    }

    @Test
    fun ownThisIsUnchanged() {
        val own = calls(type("K").findUniqueMethod("control", 0)).single()
        assertTrue((own.`object`() as VariableExpression).variable() is This)
    }

    @Test
    fun noPlaceholderOutsideTheFunctionTypeInvocation() {
        // `s.block()` (invoke on a function type with receiver) is a separate, still-open shape
        val census = PlaceholderCensus.of(types)
        assertTrue(census.dumpLines().none { !it.contains("analyze(") }, census.dumpLines().joinToString("\n"))
    }
}
