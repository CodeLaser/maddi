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

package org.e2immu.language.kotlin.k2

import org.e2immu.language.cst.api.element.DetailedSources
import org.e2immu.language.cst.api.expression.BinaryOperator
import org.e2immu.language.cst.api.expression.Cast
import org.e2immu.language.cst.api.expression.ConstructorCall
import org.e2immu.language.cst.api.expression.Equals
import org.e2immu.language.cst.api.expression.InlineConditional
import org.e2immu.language.cst.api.expression.InstanceOf
import org.e2immu.language.cst.api.expression.MethodCall
import org.e2immu.language.cst.api.expression.UnaryOperator
import org.e2immu.language.cst.api.expression.VariableExpression
import org.e2immu.language.cst.api.statement.ExpressionAsStatement
import org.e2immu.language.cst.api.statement.ReturnStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Null-safety and type operators: `!!`, `?:`, `?.`, `x as T`, `x is T`, `a[i]`. */
class NullSafetyAndCastTest : KotlinScanTestBase() {

    private fun returnedExpression(source: String, type: String = "C", method: String = "m", parameters: Int = 1) =
        (KotlinScan(runtime, sourceSet).parse("C.kt", source).first { it.simpleName() == type }
            .findUniqueMethod(method, parameters).methodBody().statements().first() as ReturnStatement).expression()

    @Test
    fun nonNullAssertionIsTransparent() {
        // `x!!` carries the same value/type -> just the variable
        val expr = returnedExpression("class C { fun m(x: String?): String { return x!! } }\n")
        assertTrue(expr is VariableExpression)
        assertNotNull(expr.source().detailedSources().detail(DetailedSources.NON_NULL_ASSERTION))
    }

    @Test
    fun asCast() {
        val expr = returnedExpression("class C { fun m(x: Any): String { return x as String } }\n")
        assertTrue(expr is Cast)
        assertEquals("String", (expr as Cast).parameterizedType().typeInfo().simpleName())
    }

    @Test
    fun isInstanceOf() {
        val expr = returnedExpression("class C { fun m(x: Any): Boolean { return x is String } }\n")
        assertTrue(expr is InstanceOf)
        assertEquals("String", (expr as InstanceOf).testType().typeInfo().simpleName())
    }

    @Test
    fun negatedIsInstanceOf() {
        // `x !is String` -> logical-not of an InstanceOf
        val expr = returnedExpression("class C { fun m(x: Any): Boolean { return x !is String } }\n")
        assertTrue(expr is UnaryOperator)
    }

    @Test
    fun arrayAccessBecomesGetCall() {
        // `h[0]` -> `h.get(0)` (get resolved on the source receiver type)
        val expr = returnedExpression(
            "class Holder { fun get(i: Int): String = \"\" }\n" +
                "class C { fun m(h: Holder): String { return h[0] } }\n"
        )
        assertTrue(expr is MethodCall)
        assertEquals("get", (expr as MethodCall).methodInfo().name())
        assertNotNull(expr.source().detailedSources().detail(DetailedSources.INDEX_ACCESS))
    }

    @Test
    fun stringIndexBecomesGetCall() {
        // `s[0]` on a String resolves via the predefined String's members (bootstrapped now), so the receiver
        // `s` is kept as the call's object -- rather than collapsing to a placeholder that drops the read.
        val expr = returnedExpression("class C { fun m(s: String): Char { return s[0] } }\n")
        assertTrue(expr is MethodCall, "was ${expr.javaClass.simpleName}: $expr")
        val call = expr as MethodCall
        assertEquals("s", (call.`object`() as VariableExpression).variable().simpleName())
        assertNotNull(call.source().detailedSources().detail(DetailedSources.INDEX_ACCESS))
    }

    @Test
    fun indexedAssignmentBecomesSetCall() {
        // `h[0] = "x"` -> `h.set(0, "x")`
        val c = KotlinScan(runtime, sourceSet).parse(
            "C.kt",
            "class Holder { fun get(i: Int): String = \"\"; fun set(i: Int, v: String) {} }\n" +
                "class C { fun m(h: Holder) { h[0] = \"x\" } }\n"
        ).first { it.simpleName() == "C" }

        val statement = c.findUniqueMethod("m", 1).methodBody().statements().first()
        val call = (statement as ExpressionAsStatement).expression() as MethodCall
        assertEquals("set", call.methodInfo().name())
        assertEquals(2, call.parameterExpressions().size) // index, value
        assertNotNull(call.source().detailedSources().detail(DetailedSources.INDEX_ACCESS))
    }

    @Test
    fun elvis() {
        // `x ?: "d"` -> `if (x == null) "d" else x`
        val expr = returnedExpression("class C { fun m(x: String?): String { return x ?: \"d\" } }\n")
        assertTrue(expr is InlineConditional)
        assertNotNull(expr.source().detailedSources().detail(DetailedSources.NULL_COALESCING))
    }

    @Test
    fun safeCall() {
        // `b?.foo()` -> `if (b == null) null else b.foo()`
        val expr = returnedExpression(
            "class Box { fun foo(): Int = 1 }\n" +
                "class C { fun m(b: Box?): Int? { return b?.foo() } }\n"
        )
        assertTrue(expr is InlineConditional)
        assertNotNull(expr.source().detailedSources().detail(DetailedSources.NULL_SAFE))
        val ifFalse = (expr as InlineConditional).ifFalse()
        assertTrue(ifFalse is MethodCall)
        assertEquals("foo", (ifFalse as MethodCall).methodInfo().name())
    }

    @Test
    fun inOperator() {
        // `x in list` -> `list.contains(x)`
        val expr = returnedExpression(
            "class C { fun m(list: List<String>, x: String): Boolean = x in list }\n", parameters = 2
        )
        assertTrue(expr is MethodCall)
        assertEquals("contains", (expr as MethodCall).methodInfo().name())
        assertEquals(1, expr.parameterExpressions().size) // the element; the collection is the receiver
    }

    @Test
    fun notInOperator() {
        // `x !in list` -> `!list.contains(x)`
        val expr = returnedExpression(
            "class C { fun m(list: List<String>, x: String): Boolean = x !in list }\n", parameters = 2
        )
        assertTrue(expr is UnaryOperator)
    }

    @Test
    fun comparisonOnComparable() {
        // `a < b` on Comparable objects -> `a.compareTo(b) < 0`
        val expr = returnedExpression(
            "class Money(val c: Int) : Comparable<Money> { override fun compareTo(other: Money): Int = c - other.c }\n" +
                "class C { fun m(a: Money, b: Money): Boolean = a < b }\n", parameters = 2
        )
        assertTrue(expr is BinaryOperator)
        val bin = expr as BinaryOperator
        assertEquals(runtime.lessOperatorInt(), bin.operator())
        assertTrue(bin.lhs() is MethodCall)
        assertEquals("compareTo", (bin.lhs() as MethodCall).methodInfo().name())
    }

    @Test
    fun structuralEqualityIsEqualsCall() {
        // Kotlin `a == b` is structural -> a.equals(b)
        val expr = returnedExpression(
            "class M(val c: Int) { override fun equals(other: Any?): Boolean = other is M && other.c == c\n" +
                "    override fun hashCode(): Int = c }\n" +
                "class C { fun m(a: M, b: M): Boolean = a == b }\n", parameters = 2
        )
        assertTrue(expr is MethodCall)
        assertEquals("equals", (expr as MethodCall).methodInfo().name())
    }

    @Test
    fun inheritedObjectEqualsResolves() {
        // a source type WITHOUT an equals override still resolves `a == b` via the bootstrapped
        // java.lang.Object.equals (source types walk up the hierarchy to the now-populated Object)
        val expr = returnedExpression(
            "class Plain(val x: Int)\n" +
                "class C { fun m(a: Plain, b: Plain): Boolean = a == b }\n", parameters = 2
        )
        assertTrue(expr is MethodCall)
        assertEquals("equals", (expr as MethodCall).methodInfo().name())
        assertEquals("java.lang.Object", expr.methodInfo().typeInfo().fullyQualifiedName())
    }

    @Test
    fun inheritedObjectToStringResolves() {
        // `p.toString()` on a source type resolves to the bootstrapped java.lang.Object.toString
        val expr = returnedExpression(
            "class Plain\nclass C { fun m(p: Plain): String = p.toString() }\n", parameters = 1
        )
        assertTrue(expr is MethodCall)
        assertEquals("toString", (expr as MethodCall).methodInfo().name())
    }

    @Test
    fun nullComparisonIsReferential() {
        // `a == null` is a reference null-check -> the Equals node (not a.equals(null))
        val expr = returnedExpression("class C { fun m(a: String?): Boolean = a == null }\n", parameters = 1)
        assertTrue(expr is Equals)
    }

    @Test
    fun referentialIdentity() {
        // `a === b` is reference identity -> the Equals node
        val expr = returnedExpression("class C { fun m(a: Any, b: Any): Boolean = a === b }\n", parameters = 2)
        assertTrue(expr is Equals)
    }

    @Test
    fun augmentedIndexedAssignment() {
        // `list[0] += 5` -> `list.set(0, list.get(0) + 5)`
        val c = KotlinScan(runtime, sourceSet).parse(
            "C.kt", "class C { fun m(list: MutableList<Int>) { list[0] += 5 } }\n"
        ).first()
        val call = (c.findUniqueMethod("m", 1).methodBody().statements().first() as ExpressionAsStatement)
            .expression() as MethodCall
        assertEquals("set", call.methodInfo().name())
        val value = call.parameterExpressions()[1] // list.get(0) + 5
        assertTrue(value is BinaryOperator)
        assertEquals(runtime.plusOperatorInt(), (value as BinaryOperator).operator())
    }

    @Test
    fun rangeOperator() {
        // `1..10` -> IntRange(1, 10)
        val expr = returnedExpression("class C { fun m(): IntRange = 1..10 }\n", parameters = 0)
        assertTrue(expr is ConstructorCall)
        assertEquals("IntRange", (expr as ConstructorCall).constructor().typeInfo().simpleName())
        assertEquals(2, expr.parameterExpressions().size)
    }
}
