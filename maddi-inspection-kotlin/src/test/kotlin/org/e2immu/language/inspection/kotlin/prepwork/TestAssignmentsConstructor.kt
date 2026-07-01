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

import org.e2immu.analyzer.modification.prepwork.variable.VariableData
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.e2immu.language.cst.api.info.MethodInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestAssignmentsConstructor`: `this(...)`/`super(...)` delegation
 * argument reads. Not ported: the `a = x; b = y` field-assignment case — Kotlin requires every property to
 * be initialized (at declaration or in the constructor), which would add an assignment the Java test does
 * not have.
 */
class TestAssignmentsConstructor : CommonKotlinPrep() {

    private fun analyse(constructor: MethodInfo): VariableData {
        doMethod(constructor)
        return VariableDataImpl.of(constructor)
    }

    private fun v(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    @Test
    fun testThisDelegation() {
        // Java: `X(int a) { this(a, 0); } X(int a, int b) { }`
        val x = parse("X.kt", "package a.b\nclass X { constructor(a: Int) : this(a, 0); constructor(a: Int, b: Int) }")
        val vd = analyse(x.findConstructor(1))
        assertEquals("0", v(vd, "a").reads().toString())
    }

    @Test
    fun testSuperCall() {
        // Java: `static class P { P(int x) {} } static class C extends P { C(int a) { super(a); } }`
        val x = parse(
            "X.kt",
            """
            package a.b
            class X {
                open class P(x: Int)
                class C : P {
                    constructor(a: Int) : super(a)
                }
            }
            """.trimIndent()
        )
        val vd = analyse(x.findSubType("C", true).findConstructor(1))
        assertEquals("0", v(vd, "a").reads().toString())
    }
}
