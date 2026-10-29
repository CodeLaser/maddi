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
import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.ParameterInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.Block
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation
import io.codelaser.maddi.cst.api.variable.FieldReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A secondary constructor's own `{ … }` body is code of that constructor: a nested block at the end of its body, after
 * the `this(...)`/`super(...)` invocation and, when it is the constructor that runs them, the `init` blocks, as kotlinc
 * runs them. Until it was converted (CodeLaser/maddi#174) only the invocation survived: a field assigned there looked
 * never assigned, and a parameter modified there looked unmodified.
 */
class SecondaryConstructorBodyTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("sc/Sc.kt", """
            package sc

            interface Greeter {
                fun hi(): String
            }
            class Plain {
                val n: Int
                constructor(n: Int) {
                    this.n = n + 1
                }
            }
            class Delegating(val m: Int) {
                var k: Int = 0
                constructor(m: Int, list: MutableList<Int>) : this(m) {
                    list.add(m)
                    k = m
                }
            }
            class WithInit {
                val s: Int
                var t: Int = 0
                init {
                    s = 6
                }
                constructor(a: Int) {
                    t = a
                    val g = object : Greeter {
                        override fun hi(): String = "w"
                    }
                    g.hi()
                }
                constructor() : this(0) {}
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    /** The fields [element] assigns. */
    private fun assigned(element: Element): List<String> {
        val names = ArrayList<String>()
        element.visit { e -> ((e as? Assignment)?.variableTarget() as? FieldReference)?.let { names += it.fieldInfo().name() }; true }
        return names
    }

    private fun <T> collect(element: Element, pick: (Element) -> T?): List<T> {
        val found = ArrayList<T>()
        element.visit { e -> pick(e)?.let { found += it }; true }
        return found
    }

    /** Every statement's index extends its enclosing statement's, and a block's are in order. */
    private fun assertIndices(block: Block, parent: String) {
        val indices = block.statements().map { it.source().index() }
        indices.forEach { assertTrue(parent.isEmpty() || it.startsWith("$parent."), "$it under $parent") }
        assertEquals(indices.sorted(), indices)
        block.statements().filterIsInstance<Block>().forEach { assertIndices(it, it.source().index()) }
    }

    @Test
    fun theBodyAssignsAndReadsTheConstructorsParameter() {
        val constructor = type("Plain").constructors().single()
        val body = constructor.methodBody()
        assertEquals(1, body.statements().size, "$body")
        assertTrue(body.statements()[0] is Block)
        assertIndices(body, "")
        assertEquals(listOf("n"), assigned(body))
        val reads = collect(body) { (it as? VariableExpression)?.variable() as? ParameterInfo }
        assertEquals(listOf(constructor.parameters().single()), reads)
    }

    @Test
    fun theBodyFollowsTheInvocation() {
        val secondary = type("Delegating").constructors().single { it.parameters().size == 2 }
        val statements = secondary.methodBody().statements()
        assertEquals(2, statements.size, "${secondary.methodBody()}")
        assertTrue(statements[0] is ExplicitConstructorInvocation, "${statements[0]}")
        assertTrue(statements[1] is Block)
        assertEquals(listOf("k"), assigned(statements[1]))
        assertIndices(secondary.methodBody(), "")
        // `list.add(m)`: the body's `list` is the constructor's own parameter
        val reads = collect(statements[1]) { (it as? VariableExpression)?.variable() as? ParameterInfo }.toSet()
        assertEquals(secondary.parameters().toSet(), reads)
    }

    @Test
    fun theBodyFollowsTheInitBlocksItRuns() {
        val (delegating, callsSuper) = type("WithInit").constructors().sortedBy { it.parameters().size }
        val statements = callsSuper.methodBody().statements()
        assertEquals(2, statements.size, "${callsSuper.methodBody()}")
        assertEquals(listOf("s"), assigned(statements[0]))
        assertEquals(listOf("t"), assigned(statements[1]))
        assertIndices(callsSuper.methodBody(), "")
        // an empty body adds nothing to the delegating constructor
        assertEquals(1, delegating.methodBody().statements().size, "${delegating.methodBody()}")
    }

    @Test
    fun anObjectExpressionInTheBodyExistsAndIsReferenced() {
        val constructor = type("WithInit").constructors().single { it.parameters().size == 1 }
        val anonymous = collect(constructor.methodBody()) { (it as? ConstructorCall)?.anonymousClass() }.single()
        val hi = anonymous.methods().single { it.name() == "hi" }
        assertSame(constructor, anonymous.enclosingMethod())
        assertEquals(setOf(type("Greeter").methods().single { it.name() == "hi" }), hi.overrides())
        assertEquals(1, constructor.source().detailedSources().references(hi).size)
    }
}
