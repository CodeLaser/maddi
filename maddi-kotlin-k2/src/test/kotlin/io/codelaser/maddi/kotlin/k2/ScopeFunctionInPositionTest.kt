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
 * Anything evaluated before it is evaluated into a temporary first (`between`), in any operand evaluated always:
 * a template, an index. Where it runs only sometimes -- under `&&`, a safe call's argument, a branch, an elvis's right
 * operand -- its statements go into the branch where it runs. A loop's condition, run on each iteration, has no
 * such place: there it stays a call (`loop`).
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
                fun whenArm(k: Int, a: StringBuilder?, b: StringBuilder?): Int? = when (k) { 1 -> { a?.let { it.length } ?: b?.let { it.length + 1 } } else -> null }
                fun lambdaTail(xs: List<StringBuilder?>, b: StringBuilder): List<StringBuilder> = xs.map { val y = it; y ?: b.also { c -> c.append("m") } }
                fun lambdaOnly(xs: List<StringBuilder?>, b: StringBuilder): List<StringBuilder> = xs.map { it ?: b.also { c -> c.append("n") } }
                fun unitElvis(a: StringBuilder?, b: StringBuilder, m: MutableMap<Int, String>) { a?.let { m[1] = "p" } ?: run { m.remove(1) } }
                fun localElvis(a: StringBuilder?, b: StringBuilder): Int { val x = a ?: b.apply { append("g") }; return x.length }
                fun take3(n: Int, x: StringBuilder, m: Int): Int = n + m
                fun between(s: StringBuilder): Int = take3(make(), s.apply { append("q") }, make())
                fun template(s: StringBuilder): String = "<${'$'}{s.apply { append("r") }}>"
                fun index(xs: IntArray, s: StringBuilder): Int = xs[s.apply { append("t") }.length]
                fun conditional(c: Boolean, s: StringBuilder): Boolean = c && take(s.apply { append("u") }) > 0
                fun safeArgument(o: B?, s: StringBuilder): Int? = o?.take2(1, s.apply { append("v") })
                fun safeStatement(o: B?, s: StringBuilder) { o?.take(s.apply { append("w") }) }
                fun ifBranch(c: Boolean, s: StringBuilder, t: StringBuilder): Int = take(if (c) s.apply { append("x") } else t)
                fun elvisDeep(a: Int?, s: StringBuilder): Int = a ?: take(s.apply { append("y") })
                fun unitBody(s: StringBuilder): Unit = with(s) { append("z") }
                fun returnedIf(c: Boolean, s: StringBuilder, t: StringBuilder): StringBuilder = if (c) { s.apply { append("+") } } else t
                fun statementIf(c: Boolean, s: StringBuilder) { if (c) s.apply { append("-") } else s }
                fun loop(s: StringBuilder) { while (take(s.apply { append("!") }) > 0) { } }
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
            "chain", "chain3", "elvisRun", "valueRun", "infix", "compared", "nested", "withACall",
            "whenArm", "lambdaTail", "lambdaOnly", "unitElvis", "between", "template", "index", "conditional",
            "safeArgument", "safeStatement", "ifBranch", "elvisDeep", "unitBody", "loop", "returnedIf", "statementIf")
        val actual = names.joinToString("\n") { "$it: ${body(it)}" }
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        // `afterACall`: `make()` runs before the lambda, so it is evaluated into a temporary first
        assertEquals("""
            argument: {s.append("a");} return take(s);
            memberArgument: {s.append("b");} return o.take2(1,s);
            statementArgument: {s.append("c");} take(s);
            afterACall: int ${'$'}value0=make(); {s.append("d");} return take2(${'$'}value0,s);
            elvis: StringBuilder ${'$'}elvis0; if(a!=null){${'$'}elvis0=a;}else{{b.append("e");}${'$'}elvis0=b;} return ${'$'}elvis0;
            safeElvis: StringBuilder ${'$'}elvis1; if(a!=null){${'$'}elvis1=a;}else{if(!(b==null)){b.append("f");}${'$'}elvis1=b;} return ${'$'}elvis1;
            localElvis: StringBuilder ${'$'}elvis8; if(a!=null){${'$'}elvis8=a;}else{{b.append("g");}${'$'}elvis8=b;} StringBuilder x=${'$'}elvis8; return x.length;
            chain: Integer ${'$'}let0=null; if(!(a==null)){${'$'}let0=a.length;} Integer ${'$'}elvis2; if(${'$'}let0!=null){${'$'}elvis2=${'$'}let0;}else{Integer ${'$'}let1=null;if(!(b==null)){${'$'}let1=b.length+1;}${'$'}elvis2=${'$'}let1;} return ${'$'}elvis2;
            chain3: Integer ${'$'}let2=null; if(!(a==null)){${'$'}let2=a.length;} Integer ${'$'}elvis3; if(${'$'}let2!=null){${'$'}elvis3=${'$'}let2;}else{Integer ${'$'}let3=null;if(!(b==null)){${'$'}let3=b.length+1;}${'$'}elvis3=${'$'}let3;} return ${'$'}elvis3==null?c:${'$'}elvis3;
            elvisRun: StringBuilder ${'$'}let4=null; if(!(a==null)){${'$'}let4=a.append("x");} if(${'$'}let4==null){{b.append("y");}}
            valueRun: StringBuilder ${'$'}elvis4; if(a!=null){${'$'}elvis4=a;}else{StringBuilder ${'$'}run5;{b.append("y");${'$'}run5=b;}${'$'}elvis4=${'$'}run5;} StringBuilder v=${'$'}elvis4; return v;
            infix: {s.append("i");} return TuplesKt.to(s,n);
            compared: String ${'$'}run6=null; if(!(s==null)){s.append("j");${'$'}run6=s.toString();} return ${'$'}run6.equals(t);
            nested: {s.append("k");} return take2(1,s)+1;
            withACall: StringBuilder ${'$'}this${'$'}with=new StringBuilder("w"); int ${'$'}with7; {${'$'}this${'$'}with.append("l");${'$'}with7=${'$'}this${'$'}with.length;} return ${'$'}with7;
            whenArm: return switch(k){case 1->{Integer ${'$'}let8=null;if(!(a==null)){${'$'}let8=a.length;}Integer ${'$'}elvis5;if(${'$'}let8!=null){${'$'}elvis5=${'$'}let8;}else{Integer ${'$'}let9=null;if(!(b==null)){${'$'}let9=b.length+1;}${'$'}elvis5=${'$'}let9;}yield ${'$'}elvis5;}default->null;};
            lambdaTail: return CollectionsKt.map(xs,it->{StringBuilder y=it;StringBuilder ${'$'}elvis6;if(y!=null){${'$'}elvis6=y;}else{{b.append("m");}${'$'}elvis6=b;}return ${'$'}elvis6;});
            lambdaOnly: return CollectionsKt.map(xs,it->{StringBuilder ${'$'}elvis7;if(it!=null){${'$'}elvis7=it;}else{{b.append("n");}${'$'}elvis7=b;}return ${'$'}elvis7;});
            unitElvis: if(!(a==null)){m.put(1,"p");}else{{m.remove(1);}}
            between: int ${'$'}value1=make(); {s.append("q");} return take3(${'$'}value1,s,make());
            template: {s.append("r");} return "<"+s+">";
            index: {s.append("t");} return xs[s.length];
            conditional: boolean ${'$'}and9=c; if(${'$'}and9){{s.append("u");}${'$'}and9=take(s)>0;} return ${'$'}and9;
            safeArgument: Integer ${'$'}safe0=null; if(!(o==null)){{s.append("v");}${'$'}safe0=o.take2(1,s);} return ${'$'}safe0;
            safeStatement: if(!(o==null)){{s.append("w");}o.take(s);}
            ifBranch: StringBuilder ${'$'}if10; if(c){{s.append("x");}${'$'}if10=s;}else{${'$'}if10=t;} return take(${'$'}if10);
            elvisDeep: int ${'$'}elvis11; if(a!=null){${'$'}elvis11=a;}else{{s.append("y");}${'$'}elvis11=take(s);} return ${'$'}elvis11;
            unitBody: {s.append("z");}
            loop: while(take(StandardKt.apply(s,${'$'}receiver->${'$'}receiver.append("!")))>0){}
            returnedIf: StringBuilder ${'$'}if12; if(c){{s.append("+");}${'$'}if12=s;}else{${'$'}if12=t;} return ${'$'}if12;
            statementIf: if(c){{s.append("-");}}else{s;}
            """.trimIndent(), actual)
    }
}
