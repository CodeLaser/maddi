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
 * @InlineOnly stdlib queries the library-call census (2026-11-16) still read as modifying: as K2 names them they are
 * private facade methods, uncontracted; kotlinc inlines the JDK call of the same meaning, which the jdk archive
 * contracts (#15). Infix spelling included (`a contentEquals b`).
 */
class InlineOnlyQueryTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("iq/Iq.kt", """
            package iq
            import java.nio.file.Path
            import kotlin.io.path.Path
            import kotlin.io.path.inputStream
            class K {
                fun eq(a: ByteArray, b: ByteArray): Boolean = a.contentEquals(b)
                fun eqInfix(a: CharArray, b: CharArray): Boolean = a contentEquals b
                fun hash(a: ByteArray): Int = a.contentHashCode()
                fun str(a: IntArray): String = a.contentToString()
                fun has(m: Map<String, Int>, k: String): Boolean = m.containsKey(k)
                fun size(c: Collection<String>): Int = c.count()
                fun open(p: Path) = p.inputStream()
                fun path(a: String, b: String): Path = Path(a, b)
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String {
        val k = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "K" }
        return k.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")
    }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theJdkCalls() {
        assertEquals("""
            eq: return Arrays.equals(a,b);
            eqInfix: return Arrays.equals(a,b);
            hash: return Arrays.hashCode(a);
            str: return Arrays.toString(a);
            has: return m.containsKey(k);
            size: return c.size();
            open: return Files.newInputStream(p);
            path: return Paths.get(a,b);
            """.trimIndent(), listOf("eq", "eqInfix", "hash", "str", "has", "size", "open", "path")
            .joinToString("\n") { "$it: ${body(it)}" })
    }
}
