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
                fun takes(f: suspend (Box) -> Int): Int = 0
                fun lam(): Int = takes { b -> leaf(b, "y") }
                suspend fun invokes(f: suspend (Box) -> Int, b: Box): Int = f(b)
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
        // `yield` is SequenceScope's suspend member: two JVM parameters; the continuation is the suspend lambda's own
        assertEquals("{return SequencesKt.sequence((\$receiver,\$completion)->\$receiver.yield(1,\$completion));}",
            k().findUniqueMethod("seq", 0).methodBody().toString())
    }

    /** A suspend function type is `Function{N+1}<…, Continuation<R>, Object>`, as in bytecode, not K2's SuspendFunctionN. */
    @Test
    fun suspendFunctionTypesAndLambdas() {
        assertEquals("takes(kotlin.jvm.functions.Function2<ss.Box,kotlin.coroutines.Continuation<Integer>,Object>):int",
            signature(k().findUniqueMethod("takes", 1)))
        assertEquals("{return takes((b,\$completion)->leaf(b,\"y\",\$completion));}", k().findUniqueMethod("lam", 0).methodBody().toString())
        // invoking a suspend function VALUE passes the continuation too
        assertEquals("{return f.invoke(b,\$completion);}", k().findUniqueMethod("invokes", 3).methodBody().toString())
    }
}
