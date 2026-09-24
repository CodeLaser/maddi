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
 * Two calls whose callee is not what the source names: a PROPERTY of function type called like a method
 * (`d.provider("x")` is `d.getProvider().invoke("x")`), and a parameter of a function type WITH a receiver invoked
 * with that receiver implicit (`init()` is `init.invoke($receiver)`). And `AutoCloseable.use`, whose facade,
 * `kotlin.jdk7.AutoCloseableKt`, lives in a JVM package that is not its Kotlin one (`@file:JvmPackageName`).
 */
class FunctionValueCallTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("fv/Fv.kt", """
            package fv
            class Desc(val provider: (String) -> Int)
            class W { fun write(s: String) {} }
            fun W.tag(name: String, init: W.() -> Unit) { write(name); init() }
            class K {
                fun viaProperty(d: Desc): Int = d.provider("x")
                fun viaUse(c: AutoCloseable): Int = c.use { 1 }
            }
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
        val actual = "viaProperty: " + body("K", "viaProperty") + "\nviaUse: " + body("K", "viaUse") +
            "\ntag: " + body("FvKt", "tag")
        assertEquals("""
            viaProperty: return d.getProvider().invoke("x");
            viaUse: return AutoCloseableKt.use(c,it->1);
            tag: ${'$'}receiver.write(name); init.invoke(${'$'}receiver);
            """.trimIndent(), actual)
    }
}
