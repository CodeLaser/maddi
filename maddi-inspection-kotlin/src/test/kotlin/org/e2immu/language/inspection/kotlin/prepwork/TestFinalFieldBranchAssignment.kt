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
 * Kotlin port of `modification-prepwork` `TestFinalFieldBranchAssignment`: effectively-final-field detection.
 * A field assigned anywhere in the primary type — including inside a `when`-branch, a lambda body, or an
 * anonymous-object method whose owner is the field's type — must not be reported final. The field is a Kotlin
 * `var` (assignable) in every case; finality is *computed* from the assignments, exactly as for the Java
 * non-final field.
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
    fun testEarlyReturnBranch() {
        assertFalse(isFinal("""
            package a.b
            class X {
                private var i: Int
                constructor(i: Int) { this.i = i }
                fun mutate(c: Boolean) { if (c) { this.i = 3; return }; println("x") }
            }
        """.trimIndent()))
    }

    @Test
    fun testWhenExpressionBranch() {
        assertFalse(isFinal("""
            package a.b
            class X {
                private var i: Int
                constructor(i: Int) { this.i = i }
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
                constructor(i: Int) { this.i = i }
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
                constructor(i: Int) { this.i = i }
                fun mutate() {
                    val r = object : Runnable { override fun run() { i = 1 } }
                    r.run()
                }
            }
        """.trimIndent()))
    }

    // NOTE: the Java suite's positive control (a non-final-*declared* field assigned only in the constructor,
    // hence *effectively* final -> true) has no faithful Kotlin equivalent. Kotlin declares mutability
    // explicitly: a `var` is declared-mutable (final=false even when only the constructor assigns it), and a
    // `val` is declared-immutable (trivially final). There is no "effectively final despite the declaration"
    // notion to reproduce, so that case is intentionally not ported.
}
