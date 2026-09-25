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
            caught: return StandardKt__StandardKt.run(this,${'$'}receiver->{try{return StringsKt__StringNumberConversionsJVMKt.toInt(s);}catch(NumberFormatException _){return 0;}});
            """.trimIndent(), listOf("html", "member", "ext", "plain", "caught").joinToString("\n") { "$it: ${body(it)}" })
    }

    /** ⚠ the primary constructor is still modelled (the JVM has none); only the synthetic `$default` one is gone. */
    @Test
    fun anAnnotationHasNoDefaultsConstructor() {
        assertEquals("", type("Paths").constructors().filter { it.isSynthetic }.joinToString(" ") { it.fullyQualifiedName() })
    }
}
