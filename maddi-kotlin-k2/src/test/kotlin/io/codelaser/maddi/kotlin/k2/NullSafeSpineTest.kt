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
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ⛔ <b>`a?.b()?.c()` used to appear in the tree 2ⁿ times.</b> A safe call and a value elvis each lower to a
 * ternary in which the tested operand stands twice — once in the null test, once in the value — and the CST
 * is a tree, so the same instance cannot serve both (#32). Converting it a second time was the answer, and
 * converting *evaluates*: down a chain each level doubled the one below it. Measured on detekt: 331 sites
 * twice over, 116 four times, 28 eight times, 6 sixteen times, 4 thirty-two times.
 *
 * <p>These count CALLS IN THE TREE against calls in the source. That is the property that was broken, and
 * neither the placeholder census nor a passing parse could see it.
 */
class NullSafeSpineTest : KotlinScanTestBase() {

    private fun callsTo(type: TypeInfo, method: String, name: String): Int {
        var n = 0
        type.findUniqueMethod(method, 0).methodBody().visit { e: Element ->
            if (e is MethodCall && e.methodInfo().name() == name) n++
            true
        }
        return n
    }

    private fun parse(body: String): TypeInfo =
        KotlinScan(runtime, sourceSet).parse("P.kt", """
            class Q { fun b(): Q? = null
                      fun c(): Q? = null
                      fun d(): Q? = null }
            class P {
                fun q(): Q? = null
                $body
            }
            """.trimIndent() + "\n").first { it.simpleName() == "P" }

    @Test
    fun aSingleSafeCallEvaluatesItsReceiverOnce() {
        val p = parse("fun f(): Q? { val r = q()?.b(); return r }")
        assertEquals(1, callsTo(p, "f", "q"), "the receiver q() is evaluated once in the source")
        assertEquals(1, callsTo(p, "f", "b"))
    }

    /** ⭐ The chain: linear, not exponential. Before the spine hoist this was 4 × q(), 2 × b(), 1 × c(). */
    @Test
    fun aChainOfSafeCallsIsLinear() {
        val p = parse("fun f(): Q? { val r = q()?.b()?.c(); return r }")
        assertEquals(1, callsTo(p, "f", "q"), "q() is called once in the source")
        assertEquals(1, callsTo(p, "f", "b"), "b() is called once in the source")
        assertEquals(1, callsTo(p, "f", "c"))
    }

    /** Three links — the shape that reached 8× and 16× on the corpus. */
    @Test
    fun aLongerChainStaysLinear() {
        val p = parse("fun f(): Q? { val r = q()?.b()?.c()?.d(); return r }")
        assertEquals(listOf(1, 1, 1, 1),
            listOf(callsTo(p, "f", "q"), callsTo(p, "f", "b"), callsTo(p, "f", "c"), callsTo(p, "f", "d")))
    }

    /** A value elvis on the spine, which is the other half of the doubling. */
    @Test
    fun aValueElvisEvaluatesItsLeftOnce() {
        val p = parse("fun f(): Q? { val r = q()?.b() ?: q(); return r }")
        assertEquals(2, callsTo(p, "f", "q"), "the source calls q() twice: once on the left, once on the right")
        assertEquals(1, callsTo(p, "f", "b"))
    }

    /**
     * ⛔ The control that fences the fix. `g()` is an ARGUMENT of a safe call: the source evaluates it only
     * when the receiver is non-null, so hoisting it above the statement would call it unconditionally. It
     * must stay inside the conditional — only the RECEIVER is on the spine.
     *
     * <p>⚠ This test first asserted `g()` appeared TWICE, on the assumption that everything inside a safe
     * call was duplicated. It is not: the lowering only ever re-converted the receiver, so the selector and
     * its arguments were always single. The blast radius of the defect was narrower than I assumed, and the
     * control now says what is true — one call, and exactly one temporary, for the receiver alone.
     */
    @Test
    fun anArgumentOfASafeCallIsNotHoisted() {
        val scan = KotlinScan(runtime, sourceSet)
        val p = scan.parse("P.kt", """
            class Q { fun b(i: Int): Q? = null }
            class P {
                fun q(): Q? = null
                fun g(): Int = 1
                fun f(): Q? { val r = q()?.b(g()); return r }
            }
            """.trimIndent() + "\n").first { it.simpleName() == "P" }
        assertEquals(1, callsTo(p, "f", "q"), "the receiver is on the spine and IS hoisted")
        assertEquals(1, callsTo(p, "f", "g"), "the argument is evaluated once, where the source evaluates it")
        assertEquals(1, scan.nullSafeTemporaries, "one temporary, for the receiver; the argument is not lifted")
    }
}
