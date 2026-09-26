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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement
import io.codelaser.maddi.cst.api.statement.IfElseStatement
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.statement.TryStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * <b>A `try` used as a VALUE, and an `if` whose branch is more than one expression.</b> The CST has neither
 * as an expression — Java does not either — so the declaration is split from the assignment and each branch
 * assigns into the local, which is what both languages compile to.
 *
 * `try` as an expression has been an open item since August (`docs/status/kotlin-corpora.md` §5.2, where it cost a
 * prep isolate on coil).
 */
class StatementAsValueTest : KotlinScanTestBase() {

    private fun parse(body: String): TypeInfo =
        KotlinScan(runtime, sourceSet).parse("P.kt", "class P {\n    $body\n}\n").first()

    @Test
    fun aTryAsAValueSplitsIntoDeclarationAndAssigningTry() {
        val p = parse("fun f(): Int { val v = try { 1 } catch (e: Exception) { 2 }; return v }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        val statements = p.findUniqueMethod("f", 0).methodBody().statements()
        assertEquals(listOf("0.0", "0.1", "1"), statements.map { it.source().index() })
        assertTrue(statements[0] is LocalVariableCreation)
        val tryStatement = statements[1]
        assertTrue(tryStatement is TryStatement, tryStatement.javaClass.toString())
        // each branch ASSIGNS the local rather than yielding a value
        val tail = (tryStatement as TryStatement).block().statements().last()
        assertTrue((tail as ExpressionAsStatement).expression() is Assignment, tail.expression().javaClass.toString())
        val catchTail = tryStatement.catchClauses().first().block().statements().last()
        assertTrue((catchTail as ExpressionAsStatement).expression() is Assignment)
    }

    @Test
    fun aMultiStatementIfBranchAssignsToo() {
        val p = parse("fun f(c: Boolean): Int { val v = if (c) { val a = 1; a + 1 } else { 2 }; return v }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        val statements = p.findUniqueMethod("f", 1).methodBody().statements()
        assertEquals(listOf("0.0", "0.1", "1"), statements.map { it.source().index() })
        assertTrue(statements[1] is IfElseStatement)
        val tail = (statements[1] as IfElseStatement).block().statements().last()
        assertTrue((tail as ExpressionAsStatement).expression() is Assignment)
    }

    /**
     * ⭐ The control. A single-expression branch became an `InlineConditional` in the previous change and
     * must STAY one: that is the better node, and splitting it would be a regression dressed as coverage.
     */
    @Test
    fun aSingleExpressionIfIsNotSplit() {
        val p = parse("fun f(c: Boolean): Int { val v = if (c) { 1 } else { 2 }; return v }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total)
        assertEquals(listOf("0", "1"),
            p.findUniqueMethod("f", 1).methodBody().statements().map { it.source().index() })
    }

    /**
     * ⭐ The shape that actually occurs: an expression-bodied function. Three of detekt's four remaining
     * try-as-a-value sites are this, and none of them reaches the block-statement path — the first cut of
     * this lowering therefore removed <b>zero</b> of them.
     */
    @Test
    fun anExpressionBodiedTryReturnsFromEachBranch() {
        val p = parse("fun g(): Int = 1\n    fun f(): Int = try { g() } catch (e: Exception) { 2 }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total, PlaceholderCensus.of(listOf(p)).byKind.toString())
        val statements = p.findUniqueMethod("f", 0).methodBody().statements()
        assertEquals(listOf("0"), statements.map { it.source().index() })
        val tryStatement = statements.single()
        assertTrue(tryStatement is TryStatement, tryStatement.javaClass.toString())
        assertTrue((tryStatement as TryStatement).block().statements().last() is ReturnStatement)
        assertTrue(tryStatement.catchClauses().first().block().statements().last() is ReturnStatement)
    }

    /** A lambda body is a statement list too, and detekt's fourth site is a `val v = try { … }` inside one. */
    @Test
    fun aTryAsAValueInsideALambdaIsLoweredToo() {
        val p = parse("fun h(f: () -> Int): Int = f()\n"
                + "    fun f(): Int = h { val v = try { 1 } catch (e: Exception) { 2 }; v }")
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(0, census.total, census.byKind.toString())
    }

    /** A `finally` never yields the value, and must not be turned into an assignment. */
    @Test
    fun aFinallyIsNotAssigned() {
        val p = parse("fun f(): Int { val v = try { 1 } catch (e: Exception) { 2 } finally { println() }; return v }")
        assertEquals(0, PlaceholderCensus.of(listOf(p)).total)
        val tryStatement = p.findUniqueMethod("f", 0).methodBody().statements()[1] as TryStatement
        val finallyTail = tryStatement.finallyBlock().statements().last()
        assertTrue((finallyTail as ExpressionAsStatement).expression() !is Assignment,
            "the finally block's value is not the try's value")
    }
}
