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

import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * coil's last three: a destructuring whose `componentN` are top-level EXTENSION operators (`IntPair`), a `@JvmStatic`
 * member extension of a companion called through its import (okio's `ByteString.Companion.encodeUtf8`), and a
 * reference to a member of the enclosing extension's receiver passed as a receiver lambda (`apply(::draw)`).
 */
class CoilLastShapesTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("cl/Cl.kt", """
            package cl
            import cl.Bs.Companion.enc
            @JvmInline value class IntPair(private val value: Long) {
                val first: Int get() = (value shr 32).toInt()
                val second: Int get() = value.toInt()
            }
            operator fun IntPair.component1() = first
            operator fun IntPair.component2() = second
            class Bs { companion object { @JvmStatic fun String.enc(): Bs = Bs() } }
            class Canvas
            interface Image { fun draw(canvas: Canvas) }
            fun Image.paint(): Canvas = Canvas().apply(::draw)
            class K {
                fun sum(p: IntPair): Int { val (a, b) = p; return a + b }
                fun hash(s: String): Bs = s.h()
                private fun String.h() = enc()
            }
            """.trimIndent() + "\n")
    }

    private fun all() = types.flatMap { it.recursiveSubTypeStream().toList() }

    private fun body(type: String, name: String): String =
        all().first { it.simpleName() == type }.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            sum: int a=ClKt.component1(p),b=ClKt.component2(p); return a+b;
            h: return Bs.Companion.enc(${'$'}receiver);
            paint: return StandardKt__StandardKt.apply(new Canvas(),${'$'}receiver::draw);
            """.trimIndent(), "sum: " + body("K", "sum") + "\nh: " + body("K", "h") + "\npaint: " + body("ClKt", "paint"))
    }

    /** Through the singleton, not the calling class's `this`: the callee is the companion's, and so is its object. */
    @Test
    fun companionMemberExtension() {
        val ret = all().first { it.simpleName() == "K" }.methods().first { it.name() == "h" }.methodBody().statements().single()
        val call = (ret as ReturnStatement).expression() as MethodCall
        assertEquals("cl.Bs.Companion.enc(String) on Bs.Companion", call.methodInfo().fullyQualifiedName() + " on " + call.`object`())
    }
}
