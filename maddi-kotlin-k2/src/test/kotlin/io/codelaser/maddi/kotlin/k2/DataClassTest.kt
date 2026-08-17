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

import org.e2immu.language.cst.api.expression.MethodCall
import org.e2immu.language.cst.api.statement.ReturnStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Data classes: K2 provides componentN/copy/getters; we synthesize structural equals/hashCode/toString. */
class DataClassTest : KotlinScanTestBase() {

    @Test
    fun syntheticMembersAndStructuralEquals() {
        val types = KotlinScan(runtime, sourceSet).parse(
            "P.kt",
            "data class Point(val x: Int, val y: Int)\n" +
                "class C { fun eq(a: Point, b: Point): Boolean = a == b }\n"
        ).associateBy { it.simpleName() }
        val point = types.getValue("Point")

        // K2-provided (componentN + copy) plus our synthesized equals/hashCode/toString, all on Point itself
        val names = point.methods().map { it.name() }.toSet()
        assertTrue(
            names.containsAll(setOf("component1", "component2", "copy", "equals", "hashCode", "toString")),
            "methods were $names"
        )

        // equals returns boolean (the corrected RecordSynthetics) and takes one parameter
        val equals = point.findUniqueMethod("equals", 1)
        assertEquals(runtime.booleanParameterizedType(), equals.returnType())

        // `a == b` on a data class resolves to Point's own structural equals, not java.lang.Object.equals
        val expr = (types.getValue("C").findUniqueMethod("eq", 2).methodBody().statements().first()
                as ReturnStatement).expression()
        assertTrue(expr is MethodCall)
        assertEquals(point, (expr as MethodCall).methodInfo().typeInfo())
    }
}
