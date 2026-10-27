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
 * An `if` whose branch is several statements, used as a VALUE: `val x = if …` and `fun f() = if …` were lowered;
 * `return if …` and a lambda's result `if` were a `k2-block-not-a-single-expression` per branch (detekt's
 * `loadConsoleReport`, `joinToString { if (…) { …; a } else b }`). Each branch now returns its tail.
 */
class ValueIfTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("vi/Vi.kt", """
            package vi
            class K {
                fun ret(c: Boolean, n: Int): Int {
                    return if (c) {
                        val d = n * 2
                        d + 1
                    } else 0
                }
                fun apply(n: Int, f: (Int) -> Int): Int = f(n)
                fun lam(n: Int): Int = apply(n) { x -> if (x > 0) { val y = x + 1; y * 2 } else { -x } }
                fun retTry(s: String): Int {
                    return try { val v = s.hashCode(); v } catch (e: Exception) { -1 }
                }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String, arity: Int) =
        types.first { it.simpleName() == "K" }.findUniqueMethod(name, arity).methodBody().toString()

    @Test
    fun eachBranchReturnsItsTail() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        assertEquals("{if(c){int d=n*2;return d+1;}else{return 0;}}", body("ret", 2))
        assertEquals("{return apply(n,x->{if(x>0){int y=x+1;return y*2;}else{return -x;}});}", body("lam", 1))
    }

    @Test
    fun aReturnedTryToo() {
        assertEquals(0, PlaceholderCensus.of(types).total)
        assertEquals(true, body("retTry", 1).startsWith("{try{int v="), body("retTry", 1))
    }
}
