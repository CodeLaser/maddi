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
 * The innermost implicit receiver wins: `level` inside a `Builder.() -> Unit` lambda is the BUILDER's, even where the
 * enclosing class declares a `level` of its own. The class-first name lookup bound it to `this.getLevel()` -- a wrong
 * read with no placeholder, and an assignment with no setter. detekt's `languageVersionSettings = …` inside
 * `buildKtSourceModule { }` in `EnvironmentFacade`, whose own `languageVersionSettings` shadowed it.
 */
class ReceiverShadowingTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("rs/Rs.kt", """
            package rs
            class Builder { var level: Int = 0 }
            fun build(block: Builder.() -> Unit): Builder = Builder().apply(block)
            class Facade {
                val level: Int get() = 9
                fun write(): Builder = build { level = 3 }
                fun read(): Int { var r = 0; build { r = level }; return r }
                fun own(): Int = level
                fun Builder.ext(): Int = level
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String =
        types.first { it.simpleName() == "Facade" }.methods().first { it.name() == name }.methodBody().statements()
            .joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theReceiverNotTheClass() {
        assertEquals("""
            write: return RsKt.build(${'$'}receiver->${'$'}receiver.level=3);
            read: int r=0; RsKt.build(${'$'}receiver->r=${'$'}receiver.level); return r;
            own: return this.getLevel();
            ext: return ${'$'}receiver.level;
            """.trimIndent(), listOf("write", "read", "own", "ext").joinToString("\n") { "$it: ${body(it)}" })
    }
}
