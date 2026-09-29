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
            "runIt", "nonLocal", "chain", "labelled", "named")
        val actual = names.joinToString("\n") { "$it: ${body(it)}" }
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        // a stable receiver (`this`, a parameter) is bound directly; a call is evaluated once into a local. A
        // `return` in the body is the enclosing function's, with no lambda to leave. A `return@let` keeps the call.
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
            labelled: return StandardKt.let(xs,it->{if(it.isEmpty()){return 0;}return it.size;});
            named: int ${'$'}let5; {${'$'}let5=xs.size;} return ${'$'}let5;
            """.trimIndent(), actual)
    }
}
