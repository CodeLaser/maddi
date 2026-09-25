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
 * `f(a, x ?: return)`: a jump in ARGUMENT position, lowered to a temporary, a guard, and the call reading the
 * temporary -- only where nothing evaluated before that argument (the receiver, earlier arguments in source order)
 * can observe the move. The two refusals keep their placeholder, and are counted.
 */
class ArgumentJumpTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("aj/Aj.kt", """
            package aj
            class Q { fun check(p: String, n: Int, m: Int) {} }
            class K {
                fun q(): Q = Q()
                fun check2(p: String, n: Int) {}
                fun positional(p: String, s: String?) { val q = Q(); q.check(p, s?.hashCode() ?: return, 2) }
                fun named(s: String?) { check2(n = s?.hashCode() ?: return, p = "x") }
                fun unstableReceiver(s: String?) { q().check("a", s?.hashCode() ?: return, 1) }
                fun unstableEarlier(s: String?) { check2(q().toString(), s?.hashCode() ?: return) }
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(name: String): String =
        type("K").methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun onlyTheRefusalsKeepAPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(listOf("unstableEarlier", "unstableReceiver"),
            census.dumpLines().map { it.split("\t")[1].substringAfterLast('.').substringBefore('(') }.sorted(),
            census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            positional: Q q=new Q(); Integer ${'$'}elvis0=s==null?null:s.hashCode(); if(${'$'}elvis0==null){return;} q.check(p,${'$'}elvis0,2);
            named: Integer ${'$'}elvis1=s==null?null:s.hashCode(); if(${'$'}elvis1==null){return;} check2("x",${'$'}elvis1);
            """.trimIndent(), listOf("positional", "named").joinToString("\n") { "$it: ${body(it)}" })
    }
}
