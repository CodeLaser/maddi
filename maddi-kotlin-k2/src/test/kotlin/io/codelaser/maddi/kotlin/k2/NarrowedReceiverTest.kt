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
 * A member called on a receiver whose K2 type is not a class: a SMART CAST, which K2 types as the intersection of
 * the declared and the tested type (`c` after `is Validatable` is `Config & Validatable`), and a TYPE PARAMETER,
 * whose members are its bounds' (`t.validate()` for `T : Validatable`; two bounds with `where`). Both mapped to a
 * type with no TypeInfo, and the member was looked up on Object.
 */
class NarrowedReceiverTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("sc/Sc.kt", """
            package sc
            interface Config { fun name(): String }
            interface Validatable { fun validate(n: Int): Int; val prio: Int }
            class K {
                fun smart(c: Config): Int = when (c) { is Validatable -> c.validate(1); else -> 0 }
                fun smartAccess(c: Config): Int = if (c is Validatable) c.prio else 0
                fun smartOther(c: Config): String = if (c is Validatable) c.name() else ""
                fun <T : Validatable> bound(t: T): Int = t.validate(2) + t.prio
                fun <T> twoBounds(t: T): Int where T : Config, T : Validatable = t.validate(3) + t.prio
                fun <T> twoA(t: T): Int where T : Config, T : Validatable = t.validate(3)
                fun <T> twoB(t: T): String where T : Config, T : Validatable = t.name()
                fun <T : Validatable> loaded(l: java.util.ArrayList<T>): Int = l.get(0).prio
                fun <T> T.implicitBound(): Int where T : Config, T : Validatable = validate(4) + prio
                fun <T> T.ib1(): Int where T : Config, T : Validatable = validate(4)
                fun <T> T.ib2(): Int where T : Config, T : Validatable = prio
                fun Config.implicitSmart(): Int = if (this is Validatable) listOf(1).sumOf { prio + it } else 0
                fun inLambda(xs: List<Config>): Boolean = xs.any { it is Validatable && it.prio > 0 }
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
        val actual = listOf("smart", "smartAccess", "smartOther", "bound", "twoBounds", "twoA", "twoB", "loaded", "implicitBound", "ib1", "ib2", "implicitSmart", "inLambda")
            .joinToString("\n") { "$it: ${body(it)}" }
        assertEquals("""
            smart: return switch(c){case Validatable it->c.validate(1);default->0;};
            smartAccess: return c instanceof Validatable?c.getPrio():0;
            smartOther: return c instanceof Validatable?c.name():"";
            bound: return t.validate(2)+t.getPrio();
            twoBounds: return t.validate(3)+t.getPrio();
            twoA: return t.validate(3);
            twoB: return t.name();
            loaded: return l.get(0).getPrio();
            implicitBound: return ${'$'}receiver.validate(4)+${'$'}receiver.getPrio();
            ib1: return ${'$'}receiver.validate(4);
            ib2: return ${'$'}receiver.getPrio();
            implicitSmart: return ${'$'}receiver instanceof Validatable?CollectionsKt.sumOfInt(CollectionsKt.map(CollectionsKt.listOf(1),it->${'$'}receiver.getPrio()+it)):0;
            inLambda: return CollectionsKt.any(xs,it->it instanceof Validatable&&it.getPrio()>0);
            """.trimIndent(), actual)
    }
}
