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

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A jump as a function's EXPRESSION body: `fun f(): Nothing = throw X()`, and `= x ?: throw X()`. The block-bodied
 * spellings were lowered (§7.37); the expression body was converted as one value, and `throw` is none.
 */
class ThrowBodyTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("tb/Tb.kt", """
            package tb
            class K {
                fun a(): Nothing = throw IllegalStateException("a")
                fun b(s: String?): String = s ?: throw IllegalStateException("b")
                fun c(s: String?): String { return s ?: throw IllegalStateException("c") }
                fun d(s: String?): String { val t = s ?: throw IllegalStateException("d"); return t }
                fun e(s: String?): Int = s?.hashCode() ?: return 0
                fun f(s: String?, b: Boolean): Int {
                    val (x, y) = @Suppress("UNUSED") if (b) { s to s } else { null } ?: return 0
                    return (x ?: "").hashCode() + (y ?: "").hashCode()
                }
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(name: String): String =
        type("K").methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            a: throw new IllegalStateException("a");
            b: if(s==null){throw new IllegalStateException("b");} return s;
            c: if(s==null){throw new IllegalStateException("c");} return s;
            d: if(s==null){throw new IllegalStateException("d");} String t=s; return t;
            e: Integer ${'$'}elvis0=s==null?null:s.hashCode(); if(${'$'}elvis0==null){return 0;} return ${'$'}elvis0;
            f: Pair<String,String> ${'$'}elvis1=b?TuplesKt.to(s,s):null; if(${'$'}elvis1==null){return 0;} String x=${'$'}elvis1.component1(),y=${'$'}elvis1.component2(); return (x==null?"":x).hashCode()+(y==null?"":y).hashCode();
            """.trimIndent(), listOf("a", "b", "c", "d", "e", "f").joinToString("\n") { "$it: ${body(it)}" })
    }
}
