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
 * #55: `v++` on a class with `operator fun inc()` was lowered as an INT increment -- `C v=c; v++;` -- the operator
 * never called and the object treated as a number. It is `v = v.inc()`, as kotlinc compiles it.
 */
class UserIncrementTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ui/Ui.kt", """
            package ui
            class C(var n: Int) { operator fun inc(): C = C(n + 1); operator fun dec(): C = C(n - 1) }
            class K {
                var f = C(0)
                fun stmt(c: C): C { var v = c; v++; return v }
                fun pre(c: C): C { var v = c; return ++v }
                fun field() { f-- }
                fun prim(i: Int): Int { var x = i; x++; return x }
                fun post(c: C): C { var v = c; return v++ }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.first { it.simpleName() == "K" }.methods()
        .first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun theOperatorIsCalled() {
        // a primitive's `++` stays Java's own
        assertEquals("""
            stmt: C v=c; v=v.inc(); return v;
            pre: C v=c; return v=v.inc();
            field: this.f=this.f.dec();
            prim: int x=i; x++; return x;
            """.trimIndent(), listOf("stmt", "pre", "field", "prim").joinToString("\n") { "$it: ${body(it)}" })
    }

    // the OLD value of a postfix increment needs a temporary: marked, never the wrong `v++` on an object
    @Test
    fun aPostfixValueIsMarked() {
        val census = PlaceholderCensus.of(types)
        assertEquals(listOf("k2-postfix-operator-value"), census.dumpLines().map { it.substringBefore('\t') })
    }
}
