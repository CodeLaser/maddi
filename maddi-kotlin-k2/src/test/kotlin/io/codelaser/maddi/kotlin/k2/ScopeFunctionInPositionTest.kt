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
 * rest of it all the same: an argument of a call on the spine after nothing but stable reads, the left operand of an
 * operator, or the right operand of an elvis on the spine, whose body then runs in the branch where the left operand
 * is null (and in a chain `a?.let { … } ?: b?.let { … } ?: c`, each link in the branch the one before leaves).
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
                fun chain(a: StringBuilder?, b: StringBuilder?): Int? = a?.let { it.length } ?: b?.let { it.length + 1 }
                fun chain3(a: StringBuilder?, b: StringBuilder?, c: Int): Int = a?.let { it.length } ?: b?.let { it.length + 1 } ?: c
                fun elvisRun(a: StringBuilder?, b: StringBuilder) { a?.let { it.append("x") } ?: run { b.append("y") } }
                fun valueRun(a: StringBuilder?, b: StringBuilder): StringBuilder { val v = a ?: run { b.append("y"); b }; return v }
                fun infix(s: StringBuilder, n: Int): Pair<StringBuilder, Int> = s.apply { append("i") } to n
                fun compared(s: StringBuilder?, t: String): Boolean = s?.run { append("j"); toString() } == t
                fun nested(s: StringBuilder): Int = take2(1, s.apply { append("k") }) + 1
                fun withACall(): Int = with(StringBuilder("w")) { append("l"); length }
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
        val names = listOf("argument", "memberArgument", "statementArgument", "afterACall", "elvis", "safeElvis", "localElvis",
            "chain", "chain3", "elvisRun", "valueRun", "infix", "compared", "nested", "withACall")
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
            localElvis: StringBuilder ${'$'}elvis6; if(a!=null){${'$'}elvis6=a;}else{{b.append("g");}${'$'}elvis6=b;} StringBuilder x=${'$'}elvis6; return x.length;
            chain: Integer ${'$'}let0=null; if(!(a==null)){${'$'}let0=a.length;} Integer ${'$'}elvis2; if(${'$'}let0!=null){${'$'}elvis2=${'$'}let0;}else{Integer ${'$'}let1=null;if(!(b==null)){${'$'}let1=b.length+1;}${'$'}elvis2=${'$'}let1;} return ${'$'}elvis2;
            chain3: Integer ${'$'}let2=null; if(!(a==null)){${'$'}let2=a.length;} Integer ${'$'}elvis3; if(${'$'}let2!=null){${'$'}elvis3=${'$'}let2;}else{Integer ${'$'}let3=null;if(!(b==null)){${'$'}let3=b.length+1;}${'$'}elvis3=${'$'}let3;} return ${'$'}elvis3==null?c:${'$'}elvis3;
            elvisRun: StringBuilder ${'$'}let4=null; if(!(a==null)){${'$'}let4=a.append("x");} StringBuilder ${'$'}elvis4; if(${'$'}let4!=null){${'$'}elvis4=${'$'}let4;}else{StringBuilder ${'$'}run5;{${'$'}run5=b.append("y");}${'$'}elvis4=${'$'}run5;} ${'$'}elvis4;
            valueRun: StringBuilder ${'$'}elvis5; if(a!=null){${'$'}elvis5=a;}else{StringBuilder ${'$'}run6;{b.append("y");${'$'}run6=b;}${'$'}elvis5=${'$'}run6;} StringBuilder v=${'$'}elvis5; return v;
            infix: {s.append("i");} return TuplesKt.to(s,n);
            compared: String ${'$'}run7=null; if(!(s==null)){s.append("j");${'$'}run7=s.toString();} return ${'$'}run7.equals(t);
            nested: {s.append("k");} return take2(1,s)+1;
            withACall: StringBuilder ${'$'}this${'$'}with=new StringBuilder("w"); int ${'$'}with8; {${'$'}this${'$'}with.append("l");${'$'}with8=${'$'}this${'$'}with.length;} return ${'$'}with8;
            """.trimIndent(), actual)
    }
}
