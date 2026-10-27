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
