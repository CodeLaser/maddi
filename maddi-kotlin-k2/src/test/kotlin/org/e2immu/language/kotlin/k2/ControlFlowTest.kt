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

import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.info.Variance
import org.e2immu.language.cst.api.expression.Assignment
import org.e2immu.language.cst.api.expression.BinaryOperator
import org.e2immu.language.cst.api.expression.EmptyExpression
import org.e2immu.language.cst.api.expression.InlineConditional
import org.e2immu.language.cst.api.expression.Lambda
import org.e2immu.language.cst.api.expression.MethodCall
import org.e2immu.language.cst.api.expression.StringConcat
import org.e2immu.language.cst.api.expression.SwitchExpression
import org.e2immu.language.cst.api.expression.VariableExpression
import org.e2immu.language.cst.api.info.ParameterInfo
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.api.statement.BreakStatement
import org.e2immu.language.cst.api.statement.DoStatement
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation
import org.e2immu.language.cst.api.statement.ExpressionAsStatement
import org.e2immu.language.cst.api.statement.ForEachStatement
import org.e2immu.language.cst.api.statement.IfElseStatement
import org.e2immu.language.cst.api.statement.LocalVariableCreation
import org.e2immu.language.cst.api.statement.ReturnStatement
import org.e2immu.language.cst.api.statement.SwitchStatementNewStyle
import org.e2immu.language.cst.api.statement.WhileStatement
import org.e2immu.language.cst.api.variable.FieldReference
import org.e2immu.language.cst.api.variable.LocalVariable
import org.e2immu.language.cst.api.variable.This
import org.e2immu.language.cst.api.type.NullableState
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/** M1 acceptance: `class Foo { fun bar(): Int = 1 }` -> a CST TypeInfo with one MethodInfo. */

/** Control flow: if/while/for and when (statement, expression, is/in conditions). */
class ControlFlowTest : KotlinScanTestBase() {

    @Test
    fun controlFlow() {
        val scan = KotlinScan(runtime, sourceSet)
        val flow = scan.parse(
            "Flow.kt",
            """
            class Flow {
                fun classify(n: Int): Int { if (n > 0) { return 1 } else { return 0 } }
                fun countdown(n: Int): Int { var i = n; while (i > 0) { i = i - 1 }; return i }
                fun sign(n: Int): Int = if (n > 0) 1 else 0
            }
            """.trimIndent() + "\n"
        ).first()

        // if statement, with a relational-operator condition
        val ifStmt = flow.findUniqueMethod("classify", 1).methodBody().statements().first()
        assertTrue(ifStmt is IfElseStatement)
        assertTrue((ifStmt as IfElseStatement).expression() is BinaryOperator)

        // while statement (after the `var i = n` local)
        assertTrue(flow.findUniqueMethod("countdown", 1).methodBody().statements()[1] is WhileStatement)

        // if as an expression -> InlineConditional
        val sign = (flow.findUniqueMethod("sign", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(sign is InlineConditional)
    }


    @Test
    fun forLoop() {
        val scan = KotlinScan(runtime, sourceSet)
        val loop = scan.parse(
            "Loop.kt",
            """
            class Loop {
                fun sum(items: List<Int>): Int {
                    var total = 0
                    for (x in items) { total = total + x }
                    return total
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val forStmt = loop.findUniqueMethod("sum", 1).methodBody().statements()[1] // after `var total = 0`
        assertTrue(forStmt is ForEachStatement)
        // loop variable `x`, and the iterated expression is the parameter `items`
        assertEquals("x", (forStmt as ForEachStatement).initializer().localVariable().simpleName())
        assertTrue(forStmt.expression() is VariableExpression)

        // body `total = total + x`: x resolves to the loop variable
        val assign = (forStmt.block().statements().first() as ExpressionAsStatement).expression() as Assignment
        val x = (assign.value() as BinaryOperator).rhs()
        assertTrue((x as VariableExpression).variable() is LocalVariable)
    }


    @Test
    fun whenStatement() {
        val scan = KotlinScan(runtime, sourceSet)
        val w = scan.parse(
            "W.kt",
            """
            class W {
                fun describe(n: Int): String {
                    var r = "?"
                    when (n) {
                        0 -> r = "zero"
                        1, 2 -> r = "small"
                        else -> r = "other"
                    }
                    return r
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val sw = w.findUniqueMethod("describe", 1).methodBody().statements()[1] // after `var r = "?"`
        assertTrue(sw is SwitchStatementNewStyle)
        assertTrue((sw as SwitchStatementNewStyle).expression() is VariableExpression) // selector is `n`
        assertEquals(3, sw.entries().size)
        assertEquals(2, sw.entries()[1].conditions().size)               // the `1, 2 ->` arm
        assertTrue(sw.entries()[2].conditions()[0] is EmptyExpression)   // the `else` arm
    }


    @Test
    fun whenExpression() {
        val scan = KotlinScan(runtime, sourceSet)
        val g = scan.parse(
            "G.kt",
            """
            class G {
                fun grade(n: Int): String = when (n) {
                    0 -> "zero"
                    1, 2 -> "small"
                    else -> "other"
                }
            }
            """.trimIndent() + "\n"
        ).first()

        // `= when (n) { … }` -> a SwitchExpression with 3 arms
        val grade = (g.findUniqueMethod("grade", 1).methodBody().statements().first() as ReturnStatement).expression()
        assertTrue(grade is SwitchExpression)
        assertEquals(3, (grade as SwitchExpression).entries().size)
    }


    @Test
    fun whenIsConditions() {
        val scan = KotlinScan(runtime, sourceSet)
        val m = scan.parse(
            "M.kt",
            """
            class M {
                fun describe(o: Any): String = when (o) {
                    is Int -> "int"
                    is String -> "string"
                    else -> "other"
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val sw = (m.findUniqueMethod("describe", 1).methodBody().statements().first() as ReturnStatement)
            .expression() as SwitchExpression
        assertEquals(3, sw.entries().size)
        // `is Int ->` is a TYPE PATTERN on the entry (modern-Java compatible), not a condition expression
        val entry = sw.entries()[0]
        assertTrue(entry.conditions().isEmpty())
        val pattern = entry.patternVariable()
        assertEquals(runtime.intParameterizedType(), pattern.localVariable().parameterizedType())
    }

}
