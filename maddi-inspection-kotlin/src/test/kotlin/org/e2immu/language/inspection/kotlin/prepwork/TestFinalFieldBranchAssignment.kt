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

package org.e2immu.language.inspection.kotlin.prepwork

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer
import org.e2immu.language.cst.impl.analysis.PropertyImpl
import org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kotlin counterpart of `modification-prepwork` `TestFinalFieldBranchAssignment`. That Java test proves that the
 * effectively-final computation detects an assignment made *outside construction* — in a `switch`-branch, a
 * lambda body, or an anonymous-object method — and demotes the field to non-final, in contrast with its control
 * (a private field assigned only in the constructor, which stays effectively final).
 *
 * The mechanism does not carry over faithfully, and the reason is instructive rather than a mere gap:
 *
 * The K2 front-end desugars a `var` property into a backing field **plus** a synthesized `setX` setter whose body
 * is `this.x = <param>`. The finality rule (`ComputePartOfConstructionFinalField`) demotes any field assigned by a
 * method that is not in `partOfConstruction`; the generated setter is exactly such a method. So **every** Kotlin
 * `var` is reported non-final — independent of whether anything ever calls the setter, and independent of any
 * branch/lambda/anon assignment. Conversely a `val` synthesizes only a getter, so it stays final.
 *
 * Consequence: the Java test's three "assigned outside construction" cases cannot be reproduced as genuine checks
 * here — a `var` is already non-final before the branch assignment is even considered, and a `val` cannot be
 * reassigned in a branch at all. What we *can* assert faithfully is the actual Kotlin invariant this exposes:
 * `val` ⟹ final, `var` ⟹ non-final (via its setter), the latter holding even when the only assignment is in the
 * constructor. [testValIsFinal] is the control; [testVarWithConstructorAssignmentOnly] isolates the setter as the
 * true cause; the remaining cases confirm the invariant is stable across branch / lambda / anon assignments.
 */
class TestFinalFieldBranchAssignment : CommonKotlinPrep() {

    private fun isFinal(input: String): Boolean {
        val x = parse("X.kt", input)
        PrepAnalyzer(runtime).doPrimaryType(x)
        val i = x.getFieldByName("i", true)
        assertTrue(i.analysis().haveAnalyzedValueFor(PropertyImpl.FINAL_FIELD), "FINAL_FIELD not computed")
        return i.analysis().getOrDefault(PropertyImpl.FINAL_FIELD, FALSE).isTrue
    }

    @Test
    fun testValIsFinal() {
        // control: a `val` synthesizes no setter, so nothing assigns it outside construction -> final
        assertTrue(isFinal("""
            package a.b
            class X {
                private val i: Int
                constructor(iv: Int) { this.i = iv }
            }
        """.trimIndent()))
    }

    @Test
    fun testVarWithConstructorAssignmentOnly() {
        // the crux: even assigned ONLY in the constructor, a `var` is non-final -- its synthesized `setI`
        // is an assigning method outside construction. This alone (no branch/lambda/anon) already demotes it.
        assertFalse(isFinal("""
            package a.b
            class X {
                private var i: Int
                constructor(iv: Int) { this.i = iv }
            }
        """.trimIndent()))
    }

    @Test
    fun testWhenExpressionBranch() {
        assertFalse(isFinal("""
            package a.b
            class X {
                private var i: Int
                constructor(iv: Int) { this.i = iv }
                fun mutate(v: Int) {
                    val x = when (v) { 1 -> { this.i = 1; 1 } else -> 2 }
                    println(x)
                }
            }
        """.trimIndent()))
    }

    @Test
    fun testLambdaBody() {
        assertFalse(isFinal("""
            package a.b
            class X {
                private var i: Int
                constructor(iv: Int) { this.i = iv }
                fun mutate() {
                    val r = Runnable { this.i = 1 }
                    r.run()
                }
            }
        """.trimIndent()))
    }

    @Test
    fun testAnonymousObjectMethod() {
        assertFalse(isFinal("""
            package a.b
            class X {
                private var i: Int
                constructor(iv: Int) { this.i = iv }
                fun mutate() {
                    val r = object : Runnable { override fun run() { i = 1 } }
                    r.run()
                }
            }
        """.trimIndent()))
    }
}
