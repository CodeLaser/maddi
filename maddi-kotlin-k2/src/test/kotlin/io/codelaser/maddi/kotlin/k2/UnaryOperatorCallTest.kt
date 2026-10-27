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
 * Unary operators that are CALLS: kotlinx.html's `+"text"` (a member extension `unaryPlus` on the implicit tag,
 * detekt HtmlOutputReport), a member `unaryMinus()`, a top-level extension one; `+i` on an Int is `i`. Also the
 * value of a lambda that is a `try` (detekt `runCatching { try { … } catch … }`), and an annotation class with a
 * defaulted element, which kotlinc gives no `$default` constructor.
 */
class UnaryOperatorCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("uo/Uo.kt", """
            package uo
            class Tag { val parts = mutableListOf<String>(); operator fun String.unaryPlus() { parts.add(this) } }
            fun tag(body: Tag.() -> Unit): Tag = Tag().apply(body)
            class V(val x: Int) { operator fun unaryMinus(): V = V(-x) }
            class W(val x: Int)
            operator fun W.unaryMinus(): W = W(-x)
            annotation class Paths(val paths: Array<String> = [])
            class K {
                fun html(): Tag = tag { +"doc" }
                fun member(v: V): V = -v
                fun ext(w: W): W = -w
                fun plain(i: Int): Int = +i + -i
                fun caught(s: String): Int = run { try { s.toInt() } catch (_: NumberFormatException) { 0 } }
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
            html: return UoKt.tag(${'$'}receiver->${'$'}receiver.unaryPlus("doc"));
            member: return v.unaryMinus();
            ext: return UoKt.unaryMinus(w);
            plain: return i+-i;
            caught: int ${'$'}run0; {try{${'$'}run0=StringsKt.toInt(s);}catch(NumberFormatException _){${'$'}run0=0;}} return ${'$'}run0;
            """.trimIndent(), listOf("html", "member", "ext", "plain", "caught").joinToString("\n") { "$it: ${body(it)}" })
    }

    /** ⚠ the primary constructor is still modelled (the JVM has none); only the synthetic `$default` one is gone. */
    @Test
    fun anAnnotationHasNoDefaultsConstructor() {
        assertEquals("", type("Paths").constructors().filter { it.isSynthetic }.joinToString(" ") { it.fullyQualifiedName() })
    }
}
