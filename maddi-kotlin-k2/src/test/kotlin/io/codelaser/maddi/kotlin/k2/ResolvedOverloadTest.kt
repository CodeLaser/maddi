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
