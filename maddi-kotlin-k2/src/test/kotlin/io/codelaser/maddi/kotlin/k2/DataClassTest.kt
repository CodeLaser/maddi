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

import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.statement.ReturnStatement
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

    /**
     * K2 gives a data class's generated members the PSI they were generated FROM: `componentN()` the constructor
     * parameter, `copy()` the class. Registered as that PSI's reference target and host, they overwrote the real
     * ones -- so every reference to a data class's constructor property named `component1()`, and a call of its
     * constructor named `copy()`. A property rename then found no reference to the property at all (#38). A
     * generated member spells nothing, and it is not what that PSI declares.
     */
    @Test
    fun aReferenceToADataClassPropertyNamesTheProperty() {
        val types = KotlinScan(runtime, sourceSet).parse("y/Y.kt", """
            package y

            data class Node(val indent: Int = 0)

            fun Node.deeper(): Node = Node(indent = indent + 1)
            fun Node.explicit(): Int = this.indent
            fun other(n: Node): Int = n.indent
            """.trimIndent() + "\n")
        val all = types.flatMap { it.recursiveSubTypeStream().toList() }
        val node = all.single { it.simpleName() == "Node" }
        val facade = all.single { it.simpleName() == "YKt" }
        val indent = node.fields().single { it.name() == "indent" }
        val constructor = node.constructors().single { it.parameters().size == 1 }
        fun host(name: String) = facade.methods().single { it.name() == name }
        fun refs(host: Info, target: Info) =
            host.source().detailedSources()?.references(target).orEmpty().map { "${it.beginLine()}:${it.beginPos()}" }.sorted()

        assertEquals(listOf("5:32", "5:41"), refs(host("deeper"), indent), "the named argument and the bare read")
        assertEquals(listOf("5:27"), refs(host("deeper"), constructor), "`Node(` is the constructor")
        assertEquals(listOf("6:33"), refs(host("explicit"), indent), "`this.indent`")
        assertEquals(listOf("7:29"), refs(host("other"), indent), "`n.indent`")

        val generated = node.methods().filter { it.name() == "component1" || it.name() == "copy" }
        assertEquals(2, generated.size)
        for (g in generated) {
            assertEquals(listOf<String>(), listOf(host("deeper"), host("explicit"), host("other")).flatMap { refs(it, g) },
                "${g.name()} is generated: no text names it")
        }
    }

    /**
     * The BODIES kotlinc generates: `componentN()` returns the Nth primary-constructor property, `copy(…)` constructs
     * from its parameters. Both were empty -- converted from the PSI they were generated from -- so the analysis read
     * `val (a, b) = p` as reading nothing of `p` (ws/object's SARIF thread). A body property is no component.
     */
    @Test
    fun componentAndCopyBodies() {
        val types = KotlinScan(runtime, sourceSet).parse("Q.kt", """
            data class Pair2<A>(val first: A, var second: Int, val flag: Boolean = false) { val extra: Int = 3 }
            """.trimIndent() + "\n").associateBy { it.simpleName() }
        val pair = types.getValue("Pair2")
        assertEquals("""
            component1: [return this.first;]
            component2: [return this.second;]
            component3: [return this.flag;]
            copy: [return new Pair2(first,second,flag);]
            """.trimIndent(), listOf("component1", "component2", "component3", "copy").joinToString("\n") { n ->
            "$n: ${pair.methods().single { it.name() == n }.methodBody().statements()}"
        })
    }
}
