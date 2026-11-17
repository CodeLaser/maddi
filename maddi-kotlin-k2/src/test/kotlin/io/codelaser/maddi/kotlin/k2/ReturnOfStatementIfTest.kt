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
 * `return if (c) { a } else { val x = …; return x } as T`: the `if` is the operand of a cast, and its else block
 * declares a local and returns from the function itself, so it has no expression form. kotlinc evaluates it into a
 * temporary assigned in the branch that does not jump; so does this ([spineTryLowering]). Exposed's QueryAlias.get
 * (CodeLaser/maddi#220).
 */
class ReturnOfStatementIfTest : KotlinScanTestBase() {

    private val source = """
        class P {
            fun <T> q(o: T, a: T, b: T?): T {
                @Suppress("UNCHECKED_CAST")
                return if (o is String) {
                    a
                } else {
                    val x = b
                    return x ?: o
                } as T
            }
        }
        """.trimIndent() + "\n"

    @Test
    fun theIfIsHoisted() {
        val p = KotlinScan(runtime, sourceSet).parse("P.kt", source).first()
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        assertEquals("{Object \$if0;if(o instanceof String){\$if0=a;}else{Object x=b;return x==null?o:x;}return (Object)\$if0;}",
            p.findUniqueMethod("q", 3).methodBody().toString())
    }
}
