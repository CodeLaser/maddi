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
 * Four shapes behind detekt's and coil's operator placeholders, each lowered to the Java a human writes:
 * `a.size` on an array is `a.length`; `==` between booleans is the primitive `==`; `a..<b` is `IntRange(a, b - 1)`;
 * and `for ((a, b) in xs)` destructures its loop variable, whose entries were never declared -- so `lines >
 * allowedLines` compared an unresolved name. (`Map.Entry` for-loops are in run-kotlin's TestDestructuring: this
 * fixture's java.util.Map.Entry has no members.)
 */
class OperatorLoweringTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ol/Ol.kt", """
            package ol
            data class P(val a: Int, val b: String)
            class K(private val allowed: Int) {
                fun arr(a: IntArray): Boolean = a.size > 1
                fun objArr(a: Array<String>): Int = a.size
                fun bools(a: Boolean, b: Boolean, c: Boolean): Boolean = a == b == c
                fun notEq(a: Boolean, b: Boolean): Boolean = a != b
                fun until(n: Int): Int { var s = 0; for (i in 0..<n) s += i; return s }
                fun loop(ps: List<P>): Int { var n = 0; for ((a, _) in ps) { if (a > allowed) n++ }; return n }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String, arity: Int) =
        types.first { it.simpleName() == "K" }.findUniqueMethod(name, arity).methodBody().toString()

    @Test
    fun eachIsTheJava() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        assertEquals("{return a.length>1;}", body("arr", 1))
        assertEquals("{return a.length;}", body("objArr", 1))
        assertEquals("{return a==b==c;}", body("bools", 3))
        assertEquals("{return a!=b;}", body("notEq", 2))
        assertEquals("{int s=0;for(int i:new IntRange(0,n-1)){s+=i;}return s;}", body("until", 1))
        assertEquals("{int n=0;for(P \$dstr:ps){int a=\$dstr.component1();if(a>this.allowed){n++;}}return n;}", body("loop", 1))
    }
}
