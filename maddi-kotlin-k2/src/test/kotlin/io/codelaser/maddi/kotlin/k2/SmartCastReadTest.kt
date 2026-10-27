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
