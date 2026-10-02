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
 * A call to a function with a VARARG parameter that also omits a defaulted argument, or passes one after the vararg.
 * The named/omitted-argument rebuild excluded every vararg callee, so `path.writeText(s)` (a charset omitted before
 * the vararg options) and `splitToSequence(".")` (two defaulted parameters after it) found no method of their
 * arity: 11 of detekt's placeholders. kotlinc passes the vararg as an array wherever the JVM parameter is a plain
 * array -- a parameter follows it, or the call binds `$default`. A `vararg xs: T` packs into a `T[]` whose
 * constructor is the ERASURE's, `Object[]`'s, as on the JVM (Exposed's generic DSL builders: a bare `T[]` constructor
 * had no type to belong to, and threw).
 */
class VarargCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("va/Va.kt", """
            package va
            fun tail(vararg xs: Int, last: Boolean): Int = if (last) xs.size else 0
            fun many(vararg xs: String, n: Int = 0): Int = xs.size + n
            fun lead(sep: String = ",", vararg parts: String): String = parts.joinToString(sep)
            fun <T> pack(vararg xs: T, n: Int): Int = xs.size + n
            class K {
                fun after(): Int = tail(1, 2, last = true)
                fun withDefault(): Int = many("a", "b")
                fun noItems(): Int = many()
                fun spread(a: Array<String>): Int = many(*a, n = 1)
                fun namedSpread(a: Array<String>): String = lead(parts = a)
                fun plain(): String = lead(";", "x", "y")
                fun <T> generic(a: T, b: T): Int = pack(a, b, n = 1)
            }
            """.trimIndent() + "\n")
    }

    @Test
    fun aTypeParameterArrayIsConstructedAsItsErasure() {
        val call = type("K").methods().first { it.name() == "generic" }.methodBody().statements().single()
        var constructor: io.codelaser.maddi.cst.api.expression.ConstructorCall? = null
        call.visit { e: io.codelaser.maddi.cst.api.element.Element ->
            if (e is io.codelaser.maddi.cst.api.expression.ConstructorCall) constructor = e
            true
        }
        assertEquals("java.lang.Object", constructor!!.constructor().typeInfo().fullyQualifiedName())
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
        val actual = listOf("after", "withDefault", "noItems", "spread", "namedSpread", "plain", "generic")
            .joinToString("\n") { "$it: ${body(it)}" }
        assertEquals("""
            after: return VaKt.tail(new int[]{1,2},true);
            withDefault: return VaKt.many${'$'}default(new String[]{"a","b"},0,2);
            noItems: return VaKt.many${'$'}default(new String[]{},0,2);
            spread: return VaKt.many(a,1);
            namedSpread: return VaKt.lead${'$'}default(null,a,1);
            plain: return VaKt.lead(";","x","y");
            generic: return VaKt.pack(new T[]{a,b},1);
            """.trimIndent(), actual)
    }
}
