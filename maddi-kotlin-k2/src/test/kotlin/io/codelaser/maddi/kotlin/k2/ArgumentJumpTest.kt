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
 * `f(a, x ?: return)`: a jump in ARGUMENT position, lowered to a temporary, a guard, and the call reading the
 * temporary. What is evaluated before that argument (the receiver, earlier arguments in source order) is evaluated
 * into a temporary first, so the move reorders nothing (#98). Only a position that runs CONDITIONALLY -- a safe
 * call's argument -- keeps its placeholder, and is counted.
 */
class ArgumentJumpTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("aj/Aj.kt", """
            package aj
            class Q { fun check(p: String, n: Int, m: Int) {} }
            class K {
                fun q(): Q = Q()
                fun check2(p: String, n: Int) {}
                fun positional(p: String, s: String?) { val q = Q(); q.check(p, s?.hashCode() ?: return, 2) }
                fun named(s: String?) { check2(n = s?.hashCode() ?: return, p = "x") }
                fun unstableReceiver(s: String?) { q().check("a", s?.hashCode() ?: return, 1) }
                fun unstableEarlier(s: String?) { check2(q().toString(), s?.hashCode() ?: return) }
                fun conditional(s: String?, q: Q?) { q?.check("a", s?.hashCode() ?: return, 1) }
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(name: String): String =
        type("K").methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun onlyTheConditionalPositionKeepsAPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(listOf("conditional"),
            census.dumpLines().map { it.split("\t")[1].substringBefore('(').substringAfterLast('.') }.sorted(),
            census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            positional: Q q=new Q(); Integer ${'$'}elvis0=s==null?null:s.hashCode(); if(${'$'}elvis0==null){return;} q.check(p,${'$'}elvis0,2);
            named: Integer ${'$'}elvis1=s==null?null:s.hashCode(); if(${'$'}elvis1==null){return;} check2("x",${'$'}elvis1);
            unstableReceiver: Q ${'$'}value0=q(); Integer ${'$'}elvis2=s==null?null:s.hashCode(); if(${'$'}elvis2==null){return;} ${'$'}value0.check("a",${'$'}elvis2,1);
            unstableEarlier: String ${'$'}value1=q().toString(); Integer ${'$'}elvis3=s==null?null:s.hashCode(); if(${'$'}elvis3==null){return;} check2(${'$'}value1,${'$'}elvis3);
            """.trimIndent(), listOf("positional", "named", "unstableReceiver", "unstableEarlier").joinToString("\n") { "$it: ${body(it)}" })
    }
}
