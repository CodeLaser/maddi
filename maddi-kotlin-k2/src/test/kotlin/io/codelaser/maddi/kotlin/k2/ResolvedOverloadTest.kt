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

import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A call binds to the overload K2 resolved, even where the argument tiers of resolveCallee cannot tell the
 * candidates apart: `yieldAll` has three of one arity (Iterable, Iterator, Sequence), and a `List` argument matches
 * none of them exactly, so the first was taken -- the Iterator overload, which drains its argument (#15).
 */
class ResolvedOverloadTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ro/Ro.kt", """
            package ro
            class K(private val s: List<String>, private val q: Sequence<String>, private val i: Iterator<String>) {
                fun list(): Sequence<String> = sequence { yieldAll(s) }
                fun seq(): Sequence<String> = sequence { yieldAll(q) }
                fun iter(): Sequence<String> = sequence { yieldAll(i) }
            }
            """.trimIndent() + "\n")
    }

    private fun yieldAll(name: String): String {
        val k = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "K" }
        val found = mutableListOf<String>()
        k.methods().first { it.name() == name }.methodBody().visit { e ->
            if (e is MethodCall && e.methodInfo().name() == "yieldAll") found.add(e.methodInfo().fullyQualifiedName())
            true
        }
        return found.single()
    }

    @Test
    fun theResolvedOverload() {
        assertEquals("kotlin.sequences.SequenceScope.yieldAll(Iterable,kotlin.coroutines.Continuation)", yieldAll("list"))
        assertEquals("kotlin.sequences.SequenceScope.yieldAll(kotlin.sequences.Sequence,kotlin.coroutines.Continuation)",
            yieldAll("seq"))
        assertEquals("kotlin.sequences.SequenceScope.yieldAll(java.util.Iterator,kotlin.coroutines.Continuation)", yieldAll("iter"))
    }
}
