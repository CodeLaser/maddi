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

import io.codelaser.maddi.cst.api.expression.Assignment
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Two gaps the ws/object SARIF thread reported (2026-09-25): an assignment had no source position of its own (only its
 * statement had), and `s += p` on a collection was a numeric compound assignment to `s`. kotlinc compiles the latter
 * as `CollectionsKt.plusAssign(s, p)`, or, on a `var` of a read-only type, `s = CollectionsKt.plus(s, p)`.
 */
class AugmentedAssignmentTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("aa/Aa.kt", """
            package aa
            class C { var x = 0 }
            class K {
                fun write(c: C) { c.x = 3 }
                fun count(i: Int): Int { var n = i; n += 2; return n }
                fun text(s: String): String { var t = s; t += "!"; return t }
                fun add(p: String): Set<String> { val s = mutableSetOf<String>(); s += p; s -= "x"; return s }
                fun grow(p: String): List<String> { var l = listOf<String>(); l += p; return l }
            }
            """.trimIndent() + "\n")
    }

    private fun method(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "K" }
        .methods().first { it.name() == name }

    private fun body(name: String): String = method(name).methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun anAssignmentHasItsOwnPosition() {
        val assignment = (method("write").methodBody().statements().single() as ExpressionAsStatement).expression() as Assignment
        assertEquals(4, assignment.source().beginLine())
    }

    @Test
    fun theShapes() {
        // a primitive's and String's `+=` stay Java's compound assignment; a collection's are the operator calls
        assertEquals("""
            count: int n=i; n+=2; return n;
            text: String t=s; t+="!"; return t;
            add: Set<String> s=SetsKt__SetsKt.mutableSetOf(); CollectionsKt__MutableCollectionsKt.plusAssign(s,p); CollectionsKt__MutableCollectionsKt.minusAssign(s,"x"); return s;
            grow: List<String> l=CollectionsKt__CollectionsKt.listOf(); l=CollectionsKt___CollectionsKt.plus(l,p); return l;
            """.trimIndent(), listOf("count", "text", "add", "grow").joinToString("\n") { "$it: ${body(it)}" })
    }
}
