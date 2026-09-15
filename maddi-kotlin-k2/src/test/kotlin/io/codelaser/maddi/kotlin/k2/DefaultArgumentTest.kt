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

import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.expression.ConstructorCall
import io.codelaser.maddi.cst.api.expression.Expression
import io.codelaser.maddi.cst.api.expression.Lambda
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement
import io.codelaser.maddi.cst.api.statement.IfElseStatement
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.variable.FieldReference
import io.codelaser.maddi.cst.api.variable.This
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A call that omits an argument calls the callee's `$default` synthetic, as kotlinc compiles it: the zero value for
 * the omitted argument, and a bit mask of the omitted parameters. `f$default` evaluates each omitted default in the
 * callee's scope -- its parameters, its `this`, its receiver -- and calls `f`. The default used to be converted into
 * the caller's CST, where its names resolved among the caller's: `b = a` read the caller's `a`, and `to = items` the
 * caller's own `items`.
 */
class DefaultArgumentTest : KotlinScanTestBase() {

    private val source = """
        package d

        fun f(a: Int, b: Int = a): Int = a + b
        fun g(a: Int): Int = f(1)
        fun h(a: String): Int = f(1)
        fun named(): Int = f(b = 2, a = 1)

        class C(val items: MutableList<Int>) {
            fun add(x: Int, to: MutableList<Int> = items) { to.add(x) }
            fun log(x: Int, n: Int = count()) = x + n
            private fun count() = items.size
        }
        class D(val items: MutableList<Int>) {
            fun use(c: C) { c.add(1) }
            fun use2(c: C) = c.log(1)
        }

        open class Base { open fun greet(name: String = "you"): String = name }
        class Sub : Base() { override fun greet(name: String): String = "hi " + name }
        fun greetSub(s: Sub) = s.greet()

        open class P(val x: Int, val y: Int = x + 1)
        fun makeP() = P(3)
        class Q(z: Int) : P(z)

        fun lam(n: Int = 0, block: () -> Int) = n + block()
        fun useLam() = lam { 1 }
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet).parse("d/D.kt", source)
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }
    private fun method(type: String, name: String) = type(type).methods().single { it.name() == name }

    /** The expression [m] returns or evaluates. */
    private fun expression(m: MethodInfo): Expression = when (val s = m.methodBody().statements().single()) {
        is ReturnStatement -> s.expression()
        is ExpressionAsStatement -> s.expression()
        else -> throw AssertionError(s.toString())
    }

    private fun MethodCall.argumentsAsText() = parameterExpressions().map { it.toString() }

    @Test
    fun anOmittedArgumentCallsTheDefaultsSynthetic() {
        parse()
        val f = method("DKt", "f")
        val defaults = method("DKt", "f\$default")
        assertTrue(defaults.isSynthetic && defaults.isStatic)
        assertEquals(listOf("a", "b", "\$mask"), defaults.parameters().map { it.name() })
        for (caller in listOf("g", "h")) {
            val call = expression(method("DKt", caller)) as MethodCall
            assertSame(defaults, call.methodInfo(), caller)
            // `b` omitted: its zero value, and bit 1 of the mask -- nothing of the caller's own `a`
            assertEquals(listOf("1", "0", "2"), call.argumentsAsText(), caller)
        }
        // every argument written: `f` itself, in parameter order
        val named = expression(method("DKt", "named")) as MethodCall
        assertSame(f, named.methodInfo())
        assertEquals(listOf("1", "2"), named.argumentsAsText())
    }

    @Test
    fun theDefaultIsEvaluatedInTheCalleesScope() {
        parse()
        val f = method("DKt", "f")
        val defaults = method("DKt", "f\$default")
        val (a, b, mask) = defaults.parameters()
        val statements = defaults.methodBody().statements()
        assertEquals(2, statements.size)
        val ifOmitted = statements[0] as IfElseStatement
        assertTrue(ifOmitted.expression().toString().contains("\$mask&2"), ifOmitted.expression().toString())
        assertTrue((ifOmitted.expression().toString()).contains(mask.name()))
        val assignment = (ifOmitted.block().statements().single() as ExpressionAsStatement).expression() as Assignment
        assertSame(b, assignment.variableTarget())
        assertSame(a, (assignment.value() as VariableExpression).variable(), "`b = a` reads f's own a")
        // the default keeps its own position, in the callee's file: `a` in `b: Int = a`
        assertEquals(listOf(3, 24), listOf(assignment.value().source().beginLine(), assignment.value().source().beginPos()))
        val call = (statements[1] as ReturnStatement).expression() as MethodCall
        assertSame(f, call.methodInfo())
        assertEquals(listOf(a, b), call.parameterExpressions().map { (it as VariableExpression).variable() })
    }

    @Test
    fun aMembersDefaultReadsItsOwnReceiver() {
        parse()
        val add = method("C", "add")
        val addDefaults = method("C", "add\$default")
        assertTrue(!addDefaults.isStatic)
        // `c.add(1)` from D: called on c, with no trace of D's own `items`
        val use = method("D", "use")
        val call = expression(use) as MethodCall
        assertSame(addDefaults, call.methodInfo())
        assertSame(use.parameters()[0], (call.`object`() as VariableExpression).variable())
        assertEquals(listOf("1", "null", "2"), call.argumentsAsText())
        // `to = items` is this.items, where this is the C the call is made on
        val assignment = ((addDefaults.methodBody().statements()[0] as IfElseStatement).block().statements().single()
            as ExpressionAsStatement).expression() as Assignment
        val items = (assignment.value() as VariableExpression).variable() as FieldReference
        assertSame(type("C").fields().single { it.name() == "items" }, items.fieldInfo())
        assertTrue(items.isDefaultScope || (items.scope() as VariableExpression).variable() is This, items.toString())
        assertSame(add, ((addDefaults.methodBody().statements()[1] as ExpressionAsStatement).expression() as MethodCall).methodInfo())
        // `n = count()` calls C's private count, on the receiver, from C's log$default
        val logDefaults = method("C", "log\$default")
        assertSame(logDefaults, (expression(method("D", "use2")) as MethodCall).methodInfo())
        val count = ((logDefaults.methodBody().statements()[0] as IfElseStatement).block().statements().single()
            as ExpressionAsStatement).expression().let { (it as Assignment).value() }
        assertTrue(count is MethodCall, count.toString())
        count as MethodCall
        assertSame(method("C", "count"), count.methodInfo())
    }

    @Test
    fun anOverrideUsesTheDefaultsOfWhatItOverrides() {
        parse()
        val call = expression(method("DKt", "greetSub")) as MethodCall
        assertSame(method("Base", "greet\$default"), call.methodInfo())
        assertTrue(type("Sub").methods().none { it.name() == "greet\$default" })
        assertEquals(listOf("null", "1"), call.argumentsAsText())
    }

    @Test
    fun aConstructorsDefaultsAreASyntheticConstructor() {
        parse()
        val p = type("P")
        val (declared, defaults) = p.constructors().also { assertEquals(2, it.size) }
        assertTrue(defaults.isSynthetic && !declared.isSynthetic)
        assertEquals(listOf("x", "y", "\$mask", "\$marker"), defaults.parameters().map { it.name() })
        val made = expression(method("DKt", "makeP")) as ConstructorCall
        assertSame(defaults, made.constructor())
        assertEquals(listOf("3", "0", "2", "null"), made.parameterExpressions().map { it.toString() })
        // `y = x + 1` in the synthetic constructor, then this(x, y)
        val statements = defaults.methodBody().statements()
        assertEquals(2, statements.size)
        val invocation = statements[1] as ExplicitConstructorInvocation
        assertTrue(!invocation.isSuper)
        assertSame(declared, invocation.methodInfo())
        // a superclass call in a header omitting an argument invokes it too
        val q = type("Q").constructors().single()
        val superCall = q.methodBody().statements().first() as ExplicitConstructorInvocation
        assertTrue(superCall.isSuper)
        assertSame(defaults, superCall.methodInfo())
        assertEquals(4, superCall.parameterExpressions().size)
    }

    @Test
    fun aTrailingLambdaFillsTheLastParameter() {
        parse()
        val call = expression(method("DKt", "useLam")) as MethodCall
        assertSame(method("DKt", "lam\$default"), call.methodInfo())
        assertEquals(3, call.parameterExpressions().size)
        assertEquals("0", call.parameterExpressions()[0].toString())
        assertTrue(call.parameterExpressions()[1] is Lambda)
        assertEquals("1", call.parameterExpressions()[2].toString())
    }
}
