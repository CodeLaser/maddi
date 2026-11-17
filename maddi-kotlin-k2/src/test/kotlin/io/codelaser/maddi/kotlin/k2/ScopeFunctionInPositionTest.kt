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
 * #98: a scope function with a literal lambda that is not on its statement's spine, but is evaluated before the
 * rest of it all the same: an argument of the statement's call after nothing but stable reads, or the right operand
 * of an elvis on the spine, whose body then runs in the branch where the left operand is null.
 */
class ScopeFunctionInPositionTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("sp/Sp.kt", """
            package sp
            class B {
                fun take(x: StringBuilder): Int = 1
                fun take2(n: Int, x: StringBuilder): Int = n
                fun make(): Int = 2
                fun argument(s: StringBuilder): Int = take(s.apply { append("a") })
                fun memberArgument(s: StringBuilder, o: B): Int = o.take2(1, s.also { it.append("b") })
                fun statementArgument(s: StringBuilder) { take(s.apply { append("c") }) }
                fun afterACall(s: StringBuilder): Int = take2(make(), s.apply { append("d") })
                fun elvis(a: StringBuilder?, b: StringBuilder): StringBuilder = a ?: b.also { it.append("e") }
                fun safeElvis(a: StringBuilder?, b: StringBuilder?): StringBuilder? = a ?: b?.apply { append("f") }
                fun localElvis(a: StringBuilder?, b: StringBuilder): Int { val x = a ?: b.apply { append("g") }; return x.length }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "B" }.methods().first { it.name() == name }.methodBody().statements()
        .joinToString(" ")

    @Test
    fun theShapes() {
        val census = PlaceholderCensus.of(types)
        val names = listOf("argument", "memberArgument", "statementArgument", "afterACall", "elvis", "safeElvis", "localElvis")
        val actual = names.joinToString("\n") { "$it: ${body(it)}" }
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        // `afterACall`: `make()` runs before the lambda, so inlining it first would move that call; it stays a call
        assertEquals("""
            argument: {s.append("a");} return take(s);
            memberArgument: {s.append("b");} return o.take2(1,s);
            statementArgument: {s.append("c");} take(s);
            afterACall: return take2(make(),StandardKt.apply(s,${'$'}receiver->${'$'}receiver.append("d")));
            elvis: StringBuilder ${'$'}elvis0; if(a!=null){${'$'}elvis0=a;}else{{b.append("e");}${'$'}elvis0=b;} return ${'$'}elvis0;
            safeElvis: StringBuilder ${'$'}elvis1; if(a!=null){${'$'}elvis1=a;}else{if(!(b==null)){b.append("f");}${'$'}elvis1=b;} return ${'$'}elvis1;
            localElvis: StringBuilder ${'$'}elvis2; if(a!=null){${'$'}elvis2=a;}else{{b.append("g");}${'$'}elvis2=b;} StringBuilder x=${'$'}elvis2; return x.length;
            """.trimIndent(), actual)
    }
}
