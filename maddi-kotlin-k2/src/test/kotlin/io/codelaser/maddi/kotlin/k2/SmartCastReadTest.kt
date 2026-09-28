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
 * #67: a value read after a smart cast is cast to the type Kotlin reads it as, as kotlinc checkcasts it: as an
 * operand (`o > 10` on a smart-cast Int was a `k2-unsupported-operator` placeholder), as a receiver, returned, and
 * passed. The `is Int` pattern tests Integer. A nullability-only smart cast (`s != null`) adds no cast.
 */
class SmartCastReadTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("sm/Sm.kt", """
            package sm
            class K {
                fun compare(o: Any?): Any? = when (o) { is Int -> if (o > 10) 10 else o; else -> null }
                fun primitive(o: Any): Int = when (o) { is Int -> 1; else -> 0 }
                fun member(o: Any): Any = when (o) { is StringBuilder -> o.append("x"); else -> o }
                fun smartIf(o: Any): StringBuilder? { if (o is StringBuilder) return o; return null }
                fun smartArg(o: Any): Int { if (o is String) return len(o); return 0 }
                fun len(s: String): Int = 1
                fun nullable(s: String?): Int { if (s != null) return len(s); return 0 }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }.methodBody().statements()
        .joinToString(" ")

    @Test
    fun theShapes() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        val actual = listOf("compare", "primitive", "member", "smartIf", "smartArg", "nullable")
            .joinToString("\n") { "$it: ${body(it)}" }
        assertEquals("""
            compare: return switch(o){case Integer it->(Integer)o>10?10:(Integer)o;default->null;};
            primitive: return switch(o){case Integer it->1;default->0;};
            member: return switch(o){case StringBuilder it->((StringBuilder)o).append("x");default->o;};
            smartIf: if(o instanceof StringBuilder){return (StringBuilder)o;} return null;
            smartArg: if(o instanceof String){return len((String)o);} return 0;
            nullable: if(!(s==null)){return len(s);} return 0;
            """.trimIndent(), actual)
    }
}
