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

import io.codelaser.maddi.cst.api.expression.Lambda
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.impl.output.QualificationImpl
import io.codelaser.maddi.cst.print.kotlin.KotlinStatementPrinter
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * A `return` inside a lambda passed to an `inline` function can return from an ENCLOSING function (a bare
 * `return`) or an enclosing lambda (`return@outer`). The CST used to make every one of them a return from the
 * innermost lambda, which is a different program; [ReturnStatement.exitLevels] now counts the lambdas it leaves.
 */
class NonLocalReturnTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("nl/Nl.kt", """
            package nl
            class K {
                fun bare(xs: List<Int>): Boolean { xs.forEach { if (it < 0) return false }; return true }
                fun local(xs: List<Int>) { xs.forEach { if (it < 0) return@forEach; println(it) } }
                fun viaRun(x: Int): Int { run { if (x < 0) return 1 }; return 2 }
                fun nested(xs: List<Int>, ys: List<Int>): Int {
                    xs.forEach { x -> ys.forEach { y -> if (y == 0) return@forEach; if (x == y) return 1 } }
                    return 0
                }
                fun explicit(xs: List<Int>, ys: List<Int>) {
                    xs.forEach outer@{ x -> ys.forEach { y -> if (x == y) return@outer } }
                }
                fun localFun(xs: List<Int>): Int {
                    fun g(): Int { xs.forEach { if (it > 0) return it }; return 0 }
                    return g()
                }
                fun single(xs: List<Int>): Int { xs.forEach { return it }; return 0 }
                fun elvis(xs: List<String>, m: Map<String, Int>): Boolean {
                    xs.forEach { val v = m[it] ?: return false; println(v) }
                    return true
                }
            }
            """.trimIndent() + "\n")
    }

    private fun method(name: String): MethodInfo = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }

    /** Every return in the method, lambdas and local functions included, as `levels[@label]`, in source order. */
    private fun returns(name: String): String {
        val out = ArrayList<String>()
        // visit descends into lambda bodies (and local functions) by itself
        method(name).methodBody().visit { e ->
            if (e is ReturnStatement) out.add(e.exitLevels().toString() + (e.goToLabel()?.let { "@$it" } ?: ""))
            true
        }
        return out.joinToString(" ")
    }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun aBareReturnInALambdaLeavesIt() {
        assertEquals("1 0", returns("bare"))
        assertEquals("1 0", returns("viaRun"))
    }

    @Test
    fun aQualifiedReturnStaysInItsLambda() {
        assertEquals("0@forEach", returns("local"))
    }

    @Test
    fun nestedLambdasCountOneLevelEach() {
        // the inner `return@forEach` matches the INNERMOST forEach; the bare return leaves both lambdas
        assertEquals("0@forEach 2 0", returns("nested"))
        assertEquals("1@outer", returns("explicit"))
    }

    @Test
    fun aLocalFunctionIsItsOwnTarget() {
        // g's forEach lambda returns from g, one level; g's `return 0` and `return g()` are their own
        assertEquals("0 0 1", returns("localFun").split(" ").sorted().joinToString(" "))
    }

    @Test
    fun anElvisGuardInALambdaReturnsFromTheFunction() {
        assertEquals("1 0", returns("elvis"))
    }

    /** Every lambda in the method, outermost first, as whether it can end an enclosing method. */
    private fun escaping(name: String): String {
        val out = ArrayList<Boolean>()
        method(name).methodBody().visit { e ->
            if (e is Lambda) out.add(e.hasNonLocalReturn())
            true
        }
        return out.joinToString(" ")
    }

    @Test
    fun whichLambdasCanEndTheirEnclosingMethod() {
        assertEquals("true", escaping("bare"))
        assertEquals("false", escaping("local"))
        // the bare return two levels deep escapes both lambdas
        assertEquals("true true", escaping("nested"))
        // `return@outer` leaves the inner lambda, and returns from the outer one: the outer lambda does not escape
        assertEquals("false true", escaping("explicit"))
        assertEquals(true, Lambda.containsNonLocalReturn(method("bare").methodBody()))
        assertEquals(false, Lambda.containsNonLocalReturn(method("explicit").methodBody()))
    }

    /** `{ return it }` returning from the enclosing function is not the expression lambda `{ it }`. */
    @Test
    fun aNonLocalReturnIsNotASingleExpressionLambda() {
        val lambdas = ArrayList<Lambda>()
        method("single").methodBody().visit { e -> if (e is Lambda) lambdas.add(e); true }
        assertEquals(1, lambdas.size)
        assertNull(lambdas.single().singleExpression())
        assertEquals("1 0", returns("single"))
    }

    /** Kotlin prints both kinds: `return@forEach` must keep its label, or it becomes a non-local return. */
    @Test
    fun theKotlinPrinterKeepsTheLabel() {
        fun printed(name: String): List<String> {
            val out = ArrayList<String>()
            method(name).methodBody().visit { e ->
                if (e is ReturnStatement) out.add(KotlinStatementPrinter.print(e, QualificationImpl.SIMPLE_NAMES).toString())
                true
            }
            return out
        }
        assertEquals(listOf("return@forEach"), printed("local"))
        assertEquals(listOf("return@outer"), printed("explicit"))
        assertEquals(listOf("return false", "return true"), printed("bare"))
    }
}
