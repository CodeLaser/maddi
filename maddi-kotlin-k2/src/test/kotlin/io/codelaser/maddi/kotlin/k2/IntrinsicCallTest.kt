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
            fun bits(a: Boolean, b: Boolean, i: Int): Int = if (a xor b) i shl 2 else (i and 3) or (i ushr 1)
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
            .joinToString("\n") { "$it: ${body("K", it)}" } + "\nput: " + body("IcKt", "put") + "\nbits: " + body("IcKt", "bits")
        assertEquals("""
            load: return a[i];
            store: a[i]="x";
            concat: return s+"x";
            boxedPlus: Integer ${'$'}nullSafe0=x==null?null:x+1; return ${'$'}nullSafe0==null?0:${'$'}nullSafe0;
            boxedNot: return ((s==null?null:StringsKt.isEmpty(s))==null?null:!(s==null?null:StringsKt.isEmpty(s))).equals(true);
            put: ${'$'}receiver[i]=v;
            bits: return a^b?i<<2:i&3|i>>>1;
            """.trimIndent(), actual)
    }
}
