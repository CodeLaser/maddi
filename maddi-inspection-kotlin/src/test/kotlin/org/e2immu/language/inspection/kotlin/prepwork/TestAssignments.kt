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
import org.e2immu.language.cst.api.statement.IfElseStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestAssignments`. Only the plain-control-flow cases are ported; the
 * Java suite's `for(int i=…)`/instanceof-pattern/array-assignment/`synchronized` cases have no faithful Kotlin
 * form (documented in `prepwork-porting.md`).
 *
 * The Java source seeds `i` from `in.length()` (a method call); the Kotlin port uses `in.hashCode()` instead —
 * also a method call reading `in` and returning an `Int`, so the read/assignment structure is identical. It
 * avoids Kotlin's `in.length` *property* form, which the front-end models as a field access (adding a
 * `String.length#in` variable Java's method call never produces).
 */
class TestAssignments : CommonKotlinPrep() {

    private fun analyse(input: String): Pair<org.e2immu.language.cst.api.info.MethodInfo, VariableData> {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("method", 1)
        doMethod(m)
        return m to VariableDataImpl.of(m)
    }

    private fun v(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    @Test
    fun test1() {
        val (method, vd) = analyse("""
            package a.b
            class X {
                fun method(`in`: String): Int {
                    val i = `in`.hashCode()
                    val j: Int
                    val k: Int
                    if (i > 0) {
                        j = 1
                        k = 2
                        if (i > 1) {
                            return k
                        }
                    } else {
                        j = 3
                    }
                    return j
                }
            }
        """.trimIndent())

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, i, j, k", vd.knownVariableNamesToString())

        val inVi = v(vd, "in")
        assertEquals("0", inVi.reads().toString())
        assertEquals("D:-, A:[]", inVi.assignments().toString())

        val iVi = v(vd, "i")
        assertEquals("3-E, 3.0.2-E", iVi.reads().toString())
        assertEquals("D:0, A:[0]", iVi.assignments().toString())

        val ifElse = method.methodBody().statements()[3] as IfElseStatement
        val jThen = VariableDataImpl.of(ifElse.block().statements()[0])
        assertEquals("D:1, A:[3.0.0]", v(jThen, "j").assignments().toString())
        val jElse = VariableDataImpl.of(ifElse.elseBlock().statements()[0])
        assertEquals("D:1, A:[3.1.0]", v(jElse, "j").assignments().toString())
        val jIf = VariableDataImpl.of(ifElse)
        assertEquals("D:1, A:[3.0.0, 3.1.0, 3=M]", v(jIf, "j").assignments().toString())

        val jVi = v(vd, "j")
        assertEquals("4", jVi.reads().toString())
        assertEquals("D:1, A:[3.0.0, 3.1.0, 3=M]", jVi.assignments().toString())

        val kVi = v(vd, "k")
        assertEquals("3.0.2.0.0", kVi.reads().toString())
        assertEquals("D:2, A:[3.0.1]", kVi.assignments().toString())

        val rv = vd.variableInfo(method.fullyQualifiedName())
        assertEquals("D:-, A:[3.0.2.0.0, 4]", rv.assignments().toString())
    }

    /**
     * `TestAssignments.test5` ("exit, not all if-else branches"): an `if` whose then-branch `throw`s and whose
     * `else if` `return`s, followed by a reachable statement — the return variable is defined on both exit paths
     * (`D:-, A:[0.0.0, 0.1.0.0.0]`) but not after. `in.hashCode()` stands in for Java's `in.length()` (both method
     * calls on `in`; avoids Kotlin's `in.length` property → field-access variable); otherwise verbatim.
     */
    @Test
    fun test5() {
        val (method, vd) = analyse("""
            package a.b
            class X {
                fun method(`in`: String?) {
                    if (`in` == null) {
                        throw UnsupportedOperationException()
                    } else if (`in`.hashCode() == 1) {
                        return
                    }
                    System.out.println("Reachable")
                }
            }
        """.trimIndent())

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, java.lang.System.out",
            vd.knownVariableNamesToString())

        val rv = vd.variableInfo(method.fullyQualifiedName())
        // 0: if; 0.0.0: throw; 0.1.0: else-if; 0.1.0.0.0: return; 1: println
        assertEquals("D:-, A:[0.0.0, 0.1.0.0.0]", rv.assignments().toString())
        assertTrue(rv.hasBeenDefined("0.0.0"))
        assertTrue(rv.hasBeenDefined("0.1.0.0.0"))
        assertFalse(rv.hasBeenDefined("1"))
    }

    /**
     * `TestAssignments.test9` ("simple re-assigns"): a `var` read in a branch (`System.out.println(i)`), then
     * reassigned, then returned — verbatim Java oracle, incl. the `java.lang.System.out` static-field variable.
     */
    @Test
    fun test9() {
        val (method, vd) = analyse("""
            package a.b
            class X {
                fun method(j: Int): Int {
                    var i = 0
                    if (j == 1) {
                        System.out.println(i)
                    }
                    i = 1
                    return i
                }
            }
        """.trimIndent())

        assertEquals("a.b.X.method(int), a.b.X.method(int):0:j, i, java.lang.System.out",
            vd.knownVariableNamesToString())

        val iVi = v(vd, "i")
        // 0: var i, 1: if, 1.0.0: System.out.println(i) reads i, 2: i=1 reassign, 3: return i
        assertEquals("1.0.0, 3", iVi.reads().toString())
        assertEquals("D:0, A:[0, 2]", iVi.assignments().toString())
        assertTrue(iVi.assignments().hasBeenAssignedAfterFor("1=M", "3", false))

        val s2 = VariableDataImpl.of(method.methodBody().statements()[2])
        assertEquals("1.0.0", v(s2, "i").reads().toString())
        assertEquals("D:0, A:[0, 2]", v(s2, "i").assignments().toString())
    }

    /**
     * `TestAssignments.test8b` ("exit, while(real condition)"): a `return` inside a `while`, then a reachable
     * statement and a final `return` — the subject is the return variable's definite assignment
     * (`D:-, A:[1.0.0, 3]`) and `hasBeenDefined` across the loop. `in.hashCode()` stands in for Java's
     * `in.length()` (both method calls on `in`); otherwise verbatim, incl. the `java.lang.System.out` variable.
     */
    @Test
    fun test8b() {
        val (method, vd) = analyse("""
            package a.b
            class X {
                fun method(`in`: String): Int {
                    var i = 0
                    while (i < `in`.hashCode()) {
                        return `in`.hashCode()
                    }
                    System.out.println("reachable")
                    return 0
                }
            }
        """.trimIndent())

        assertEquals("a.b.X.method(String), a.b.X.method(String):0:in, i, java.lang.System.out",
            vd.knownVariableNamesToString())

        val rv = vd.variableInfo(method.fullyQualifiedName())
        // 0: var i, 1: while, 1.0.0: return in.hashCode(), 2: println, 3: return 0
        assertEquals("D:-, A:[1.0.0, 3]", rv.assignments().toString())
        assertTrue(rv.hasBeenDefined("1.0.0"))
        assertTrue(rv.hasBeenDefined("3"))
        assertFalse(rv.hasBeenDefined("0"))
        assertFalse(rv.hasBeenDefined("1"))
        assertFalse(rv.hasBeenDefined("2"))
    }
}
