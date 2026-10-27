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
 * `val (a, b) = <value>` evaluates the value ONCE, into a temporary, as kotlinc does. It used to be converted once
 * and read by every entry -- the same node twice in the tree, so `val (l, r) = when { … }` printed the `when` twice.
 * `val (a, b) = f() ?: return 0` had no control-flow elvis lowering at all. And the array constructors without an
 * init lambda (`IntArray(n)`, `ByteArray(3)`, `arrayOfNulls<T>(n)`) are `new T[n]`.
 */
class DestructuringValueTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("dv/Dv.kt", """
            package dv
            class K {
                fun pair(): Pair<Int, String>? = null
                fun elvis(): Int { val (a, b) = pair() ?: return 0; return a }
                fun annotated(c: Boolean): Int { val (a, b) = @Suppress("X") if (c) { pair() } else { null } ?: return 1; return a }
                fun whenPair(c: Boolean, x: Pair<Int, Int>, y: Pair<Int, Int>): Int { val (l, r) = when { c -> x; else -> y }; return l + r }
                fun plain(x: Pair<Int, Int>): Int { val (l, r) = x; return l + r }
                fun arrays(n: Int): Int { val a = IntArray(n); val b = ByteArray(3); val c = arrayOfNulls<String>(n); return a.size + b.size + c.size }
                fun withInit(n: Int): IntArray = IntArray(n) { it * 2 }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String, arity: Int) =
        types.first { it.simpleName() == "K" }.findUniqueMethod(name, arity).methodBody().toString()

    @Test
    fun theValueIsEvaluatedOnce() {
        // an init lambda is a filling loop now, as an expression body too (ArrayInitTest)
        assertEquals(listOf<String>(), PlaceholderCensus.of(types).dumpLines().map { it.substringBefore('\t') })
        assertEquals("{Pair<Integer,String> \$elvis0=pair();if(\$elvis0==null){return 0;}" +
            "int a=\$elvis0.component1(),b=\$elvis0.component2();return a;}", body("elvis", 0))
        assertEquals("{Pair<Integer,Integer> \$destructured0=switch(true){case c->x;default->y;};" +
            "int l=\$destructured0.component1(),r=\$destructured0.component2();return l+r;}", body("whenPair", 3))
        assertEquals("{int l=x.component1(),r=x.component2();return l+r;}", body("plain", 1))
        // `@Ann if {…} else {…} ?: return` is elvis(annotated(if), return): lowered like any other. (detekt's
        // MissingUseCall differs: `if {…} else if {…} else {…} ?: return` binds the elvis to the INNER `if`, inside the
        // outer else branch -- a `return` in a value branch, which stays a placeholder.)
        assertEquals(true, body("annotated", 1).contains("{return 1;}"), body("annotated", 1))
    }

    @Test
    fun arrayConstructorsWithoutInitAreNewArrays() {
        assertEquals("{int[] a=new int[n];byte[] b=new byte[3];String[] c=new String[n];return a.length+b.length+c.length;}",
            body("arrays", 1))
    }
}
