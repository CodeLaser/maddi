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

package org.e2immu.language.inspection.kotlin

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Tier-1: feed Kotlin-derived CST into maddi's modification analyzer and check it runs. The point is not
 * (yet) to assert deep analysis results, but to prove the end-to-end path works and to surface the first
 * real gaps — the same runtime instance is shared between the Kotlin parser and the analyzer.
 */
class KotlinAnalyzerSmokeTest {

    private val runtime: Runtime = RuntimeImpl()
    private val sourceSet: SourceSet =
        SourceSetImpl.Builder().setName("source").setUri(URI.create("file:/")).build()

    @Test
    fun analyzerRunsOnAKotlinMethod() {
        val types = KotlinScan(runtime, sourceSet).parse(
            "A.kt",
            "class A { fun m(x: Int): Int { var y = x; y = y + 1; return y } }\n"
        )
        val method = types.first().findUniqueMethod("m", 1)

        // run the prep analyzer on the method (computes the per-statement variable data)
        PrepAnalyzer(runtime).doMethod(method)

        val variableData = VariableDataImpl.of(method.methodBody().lastStatement())
        assertNotNull(variableData)
        // the analyzer saw the parameter `x` and the local `y`
        val names = variableData.knownVariableNamesToString()
        assertTrue(names.contains("x"), "variables: $names")
        assertTrue(names.contains("y"), "variables: $names")
    }

    @Test
    fun analyzerRunsOnAKotlinClass() {
        // a class with a primary-constructor property (-> backing field + getter/setter) and a method
        val types = KotlinScan(runtime, sourceSet).parse(
            "Counter.kt",
            "class Counter(var count: Int) { fun inc() { count = count + 1 } }\n"
        )
        // full prep analysis over the primary type: constructor body, accessors, and inc()
        PrepAnalyzer(runtime).doPrimaryTypes(types.toSet())

        val inc = types.first().findUniqueMethod("inc", 0)
        val variableData = VariableDataImpl.of(inc.methodBody().lastStatement())
        assertNotNull(variableData)
    }

    @Test
    fun variableDataAssignmentsAreCorrect() {
        // var a = p (stmt 0); a = a + 1 (stmt 1); return a (stmt 2)
        val types = KotlinScan(runtime, sourceSet).parse(
            "X.kt",
            "class X { fun m(p: Int): Int { var a = p; a = a + 1; return a } }\n"
        )
        val m = types.first().findUniqueMethod("m", 1)
        PrepAnalyzer(runtime).doMethod(m)

        val vd = VariableDataImpl.of(m.methodBody().lastStatement())
        // `a` is defined+assigned at statement 0 and reassigned at statement 1; `p` is a read-only parameter
        assertEquals("D:0, A:[0, 1]", vd.variableInfo("a").assignments().toString())
        // the method, its parameter `p` (Kotlin Int -> JVM int), and the local `a`
        assertEquals("X.m(int), X.m(int):0:p, a", vd.knownVariableNamesToString())
    }
}
