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
 * An intrinsic spelled as a CALL: `a.get(i)` / `a.set(i, v)` on a JVM array, `s.plus(x)` on a String, and a
 * primitive member on a BOXED receiver (`x?.plus(1)`, `b?.not()`, where the safe call types the receiver `Integer`
 * or `Boolean`). The operator spellings converted; the call spellings looked for a method no class file has.
 */
class IntrinsicCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ic/Ic.kt", """
            package ic
            class K {
                fun load(a: IntArray, i: Int): Int = a.get(i)
                fun store(a: Array<String>, i: Int) { a.set(i, "x") }
                fun concat(s: String): String = s.plus("x")
                fun boxedPlus(x: Int?): Int = x?.plus(1) ?: 0
                fun boxedNot(s: String?): Boolean = s?.isEmpty()?.not() == true
            }
            fun ByteArray.put(i: Int, v: Byte) = set(i, v)
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(type: String, name: String): String =
        type(type).methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        val actual = listOf("load", "store", "concat", "boxedPlus", "boxedNot")
            .joinToString("\n") { "$it: ${body("K", it)}" } + "\nput: " + body("IcKt", "put")
        assertEquals("""
            load: return a[i];
            store: a[i]="x";
            concat: return s+"x";
            boxedPlus: Integer ${'$'}nullSafe0=x==null?null:x+1; return ${'$'}nullSafe0==null?0:${'$'}nullSafe0;
            boxedNot: return ((s==null?null:StringsKt__StringsKt.isEmpty(s))==null?null:!(s==null?null:StringsKt__StringsKt.isEmpty(s))).equals(true);
            put: ${'$'}receiver[i]=v;
            """.trimIndent(), actual)
    }
}
