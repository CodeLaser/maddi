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
        // in `read`, the `var r` the lambda assigns is a Ref holder, as kotlinc compiles it (#72)
        assertEquals("""
            write: return RsKt.build(${'$'}receiver->${'$'}receiver.level=3);
            read: IntRef r=new IntRef(); r.element=0; RsKt.build(${'$'}receiver->r.element=${'$'}receiver.level); return r.element;
            own: return this.getLevel();
            ext: return ${'$'}receiver.level;
            """.trimIndent(), listOf("write", "read", "own", "ext").joinToString("\n") { "$it: ${body(it)}" })
    }
}
