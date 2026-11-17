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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Two Exposed shapes (CodeLaser/maddi#220): an enum entry passing several arguments to a `vararg` constructor
 * (MigrationGenerator.SupportedImage), and `v[k]` on a value of a type parameter smart-cast by a bare `v as C`
 * statement, whose type is the intersection `R & C` (Referrers.getValue).
 */
class VarargEnumAndSmartCastIndexTest : KotlinScanTestBase() {

    private val source = """
        enum class Image(vararg val prefixes: String) {
            ONE("a"),
            THREE("b", "c", "d");
        }
        class C { operator fun get(k: String): Int = 1 }
        class P {
            fun <R> f(value: R, k: String): Int {
                value as C
                return value[k]
            }
        }
        """.trimIndent() + "\n"

    @Test
    fun bothConvert() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", source)
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        val p = types.first { it.simpleName() == "P" }
        assertEquals("{(C)value;return ((C)value).get(k);}", p.findUniqueMethod("f", 2).methodBody().toString())
    }
}
