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

import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A `suspend` function as kotlinc compiles it: `Object f(A a, Continuation<R> $completion)`, and every call to one
 * passes the caller's continuation last. A class-file type already has that shape, a type built by this front end
 * did not, and a call resolved against the wrong one found nothing (detekt's `yield(x)` in `sequence { }`).
 */
class SuspendSignatureTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ss/Ss.kt", """
            package ss
            class Box { fun add(s: String) {} }
            class K {
                suspend fun leaf(b: Box, t: String): Int { b.add(t); return 1 }
                suspend fun caller(b: Box): Int = leaf(b, "x") + 1
                suspend fun unit(b: Box) = b.add("u")
                suspend fun withDefault(b: Box, t: String = "d"): Int = leaf(b, t)
                suspend fun callsDefault(b: Box): Int = withDefault(b)
                fun seq(): Sequence<Int> = sequence { yield(1) }
            }
            """.trimIndent() + "\n")
    }

    private fun k(): TypeInfo = types.first { it.simpleName() == "K" }

    private fun signature(m: MethodInfo) =
        m.name() + m.parameters().joinToString(",", "(", ")") { it.parameterizedType().toString().removePrefix("Type ") } +
            ":" + m.returnType().toString().removePrefix("Type ")

    @Test
    fun theJvmSignature() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        val leaf = k().findUniqueMethod("leaf", 3)
        assertEquals("leaf(ss.Box,String,kotlin.coroutines.Continuation<Integer>):Object", signature(leaf))
        assertEquals("\$completion", leaf.parameters().last().name())
        assertEquals("unit(ss.Box,kotlin.coroutines.Continuation<Object>):Object", signature(k().findUniqueMethod("unit", 2)))
    }

    @Test
    fun aCallPassesTheCallersContinuation() {
        assertEquals("{return leaf(b,\"x\",\$completion)+1;}", k().findUniqueMethod("caller", 2).methodBody().toString())
        // a Unit suspend function's expression body is a statement, not a returned value
        assertEquals("{b.add(\"u\");}", k().findUniqueMethod("unit", 2).methodBody().toString())
    }

    @Test
    fun aDefaultKeepsTheContinuationBeforeTheMask() {
        val defaults = k().findUniqueMethod("withDefault\$default", 4)
        assertEquals("withDefault\$default(ss.Box,String,kotlin.coroutines.Continuation<Integer>,int):Object", signature(defaults))
        assertEquals("{return withDefault\$default(b,null,\$completion,2);}", k().findUniqueMethod("callsDefault", 2).methodBody().toString())
    }

    @Test
    fun aLibrarySuspendMemberResolves() {
        // `yield` is SequenceScope's suspend member: two JVM parameters. (The lambda's own continuation is step two.)
        val body = k().findUniqueMethod("seq", 0).methodBody().toString()
        assertEquals(true, body.contains("\$receiver.yield(1,"), body)
    }
}
