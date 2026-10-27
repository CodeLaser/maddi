/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * as the inlined body of the @InlineOnly `plusAssign`, `s.add(p)`, or, on a `var` of a read-only type,
 * `s = CollectionsKt.plus(s, p)`.
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
        // a primitive's and String's `+=` stay Java's compound assignment; a MutableCollection's are kotlinc's inlined
        // add/remove; a read-only `var`'s is the `plus` facade call, assigned
        assertEquals("""
            count: int n=i; n+=2; return n;
            text: String t=s; t+="!"; return t;
            add: Set<String> s=SetsKt.mutableSetOf(); s.add(p); s.remove("x"); return s;
            grow: List<String> l=CollectionsKt.listOf(); l=CollectionsKt.plus(l,p); return l;
            """.trimIndent(), listOf("count", "text", "add", "grow").joinToString("\n") { "$it: ${body(it)}" })
    }
}
