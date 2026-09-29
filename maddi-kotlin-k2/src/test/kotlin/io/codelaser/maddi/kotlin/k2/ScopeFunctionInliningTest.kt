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

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.TryStatement
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * #88: `apply`, `also`, `let`, `run` and `with` are inlined as kotlinc inlines them (@InlineOnly): the lambda's body
 * becomes statements of the enclosing method, so a modification in it is a modification of the receiver.
 */
class ScopeFunctionInliningTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("sf/Sf.kt", """
            package sf
            class B {
                private val sb = StringBuilder()
                fun make(): List<String> = listOf("a")
                fun addAll(xs: List<String>): B = apply { xs.forEach { sb.append(it) } }
                fun onParam(s: StringBuilder): StringBuilder = s.apply { append("x") }
                fun alsoIt(s: StringBuilder): StringBuilder = s.also { it.append("x") }
                fun letValue(xs: List<String>): Int = xs.let { it.size + 1 }
                fun letCall(): Int = make().let { it.size }
                fun safeLet(xs: List<String>?): Int = xs?.let { it.size } ?: 0
                fun statement(s: StringBuilder) { s.apply { append("y") } }
                fun withIt(s: StringBuilder): StringBuilder = with(s) { append("z") }
                fun runIt(s: StringBuilder): String = s.run { append("w"); toString() }
                fun nonLocal(xs: List<String>?): Int { xs?.let { return it.size }; return 0 }
                fun chain(): String = StringBuilder().apply { append("a") }.toString()
                fun labelled(xs: List<String>): Int = xs.let { if (it.isEmpty()) return@let 0; it.size }
                fun named(xs: List<String>): Int = xs.let { ys -> ys.size }
                fun elvisLet(xs: List<String>, m: Map<String, List<String>>): Int = xs.let { val v = m[it.first()] ?: return@let 0; v.size }
                fun applyReturn(s: StringBuilder): StringBuilder = s.apply { if (isEmpty()) return@apply; append("x") }
                fun safeLabelled(xs: List<String>?): Int = xs?.let { if (it.isEmpty()) return@let 1; it.size } ?: 0
                fun tailIf(xs: List<String>): Int = xs.let { if (it.isEmpty()) return@let 0 else it.size }
                fun discarded(xs: List<String>, sb: StringBuilder) { xs.let { if (it.isEmpty()) return@let; sb.append(it) } }
                fun inLoop(xs: List<String>): Int = xs.let { for (x in it) if (x.isEmpty()) return@let 0; 1 }
                fun useIt(w: java.io.StringWriter): String = w.use { it.write("x"); it.toString() }
                fun useNew(): String = java.io.StringWriter().use { it.append("a").toString() }
                fun useSafe(w: java.io.StringWriter?) { w?.use { it.write("y") } }
                fun useLabelled(w: java.io.StringWriter, b: Boolean): Int = w.use { if (b) return@use 0; it.write("z"); 1 }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "B" }.methods().first { it.name() == name }.methodBody().statements()
        .joinToString(" ")

    @Test
    fun theShapes() {
        val census = PlaceholderCensus.of(types)
        val names = listOf("addAll", "onParam", "alsoIt", "letValue", "letCall", "safeLet", "statement", "withIt",
            "runIt", "nonLocal", "chain", "labelled", "named", "elvisLet", "applyReturn", "safeLabelled", "tailIf", "discarded",
            "inLoop", "useIt", "useNew", "useSafe", "useLabelled")
        val actual = names.joinToString("\n") { "$it: ${body(it)}" }
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        // a stable receiver (`this`, a parameter) is bound directly; a call is evaluated once into a local. A
        // `return` in the body is the enclosing function's, with no lambda to leave. A `return@let v` assigns the result
        // and the rest of the body moves into the other branch; inside a loop it keeps the call. `use` is a
        // try-with-resources, its receiver the resource (#88 stage 3).
        assertEquals("""
            addAll: {CollectionsKt.forEach(xs,it->this.sb.append(it));} return this;
            onParam: {s.append("x");} return s;
            alsoIt: {s.append("x");} return s;
            letValue: int ${'$'}let0; {${'$'}let0=xs.size+1;} return ${'$'}let0;
            letCall: List<String> it=make(); int ${'$'}let1; {${'$'}let1=it.size;} return ${'$'}let1;
            safeLet: Integer ${'$'}let2=null; if(!(xs==null)){${'$'}let2=xs.size;} return ${'$'}let2==null?0:${'$'}let2;
            statement: {s.append("y");}
            withIt: StringBuilder ${'$'}with3; {${'$'}with3=s.append("z");} return ${'$'}with3;
            runIt: String ${'$'}run4; {s.append("w");${'$'}run4=s.toString();} return ${'$'}run4;
            nonLocal: if(!(xs==null)){return xs.size;} return 0;
            chain: StringBuilder ${'$'}this${'$'}apply=new StringBuilder(); {${'$'}this${'$'}apply.append("a");} return ${'$'}this${'$'}apply.toString();
            labelled: int ${'$'}let5; {if(xs.isEmpty()){${'$'}let5=0;}else{${'$'}let5=xs.size;}} return ${'$'}let5;
            named: int ${'$'}let6; {${'$'}let6=xs.size;} return ${'$'}let6;
            elvisLet: int ${'$'}let7; {List<String> ${'$'}elvis0=m.get(CollectionsKt.first(xs));if(${'$'}elvis0==null){${'$'}let7=0;}else{List<String> v=${'$'}elvis0;${'$'}let7=v.size;}} return ${'$'}let7;
            applyReturn: {if(s.length()==0){{}}else{s.append("x");}} return s;
            safeLabelled: Integer ${'$'}let8=null; if(!(xs==null)){if(xs.isEmpty()){${'$'}let8=1;}else{${'$'}let8=xs.size;}} return ${'$'}let8==null?0:${'$'}let8;
            tailIf: int ${'$'}let9; {if(xs.isEmpty()){${'$'}let9=0;}else{${'$'}let9=xs.size;}} return ${'$'}let9;
            discarded: {if(xs.isEmpty()){{}}else{sb.append(xs);}}
            inLoop: return StandardKt.let(xs,it->{for(String x:it){if(StringsKt.isEmpty(x)){return 0;}}return 1;});
            useIt: String ${'$'}use10; try(w){w.write("x");${'$'}use10=w.toString();} return ${'$'}use10;
            useNew: String ${'$'}use11; try(StringWriter it=new StringWriter()){${'$'}use11=it.append("a").toString();} return ${'$'}use11;
            useSafe: if(!(w==null)){try(w){w.write("y");}}
            useLabelled: int ${'$'}use12; try(w){if(b){${'$'}use12=0;}else{w.write("z");${'$'}use12=1;}} return ${'$'}use12;
            """.trimIndent(), actual)
    }

    @Test
    fun aResourceIsNumberedAsTheJavaParserNumbersIt() {
        val statements = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "B" }
            .methods().first { it.name() == "useNew" }.methodBody().statements()
        val tried = statements[1] as TryStatement
        assertEquals("1", tried.source().index())
        assertEquals("1+0", tried.resources().single().source().index())
        assertEquals("1.0", tried.block().source().index())
    }
}
