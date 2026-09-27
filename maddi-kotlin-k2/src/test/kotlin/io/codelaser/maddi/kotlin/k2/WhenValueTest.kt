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
 * A `when` used as a VALUE has the shape the Java parser gives a switch expression: a single-expression arm is an
 * expression statement (`case 0 -> -1`), a block arm ends in `yield`. It used to be the statement form's block
 * with a plain tail expression, so nothing marked an arm's value: the analyzer reads a switch expression's value
 * through its yields, and jfocus-transform's lowering dropped every one (javalin transform census, 2026-09-27).
 */
class WhenValueTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("wv/Wv.kt", """
            package wv
            class K {
                fun single(i: Int): String = when (i) { 0 -> "zero"; else -> "other" }
                fun block(i: Int): Int = when (i) {
                    0 -> { println("zero"); 10 }
                    else -> { val j = i * 2; j + 1 }
                }
                fun jump(i: Int): Int = when (i) { 0 -> throw IllegalStateException(); else -> i }
                fun statement(i: Int) { when (i) { 0 -> println("zero"); else -> println("other") } }
                fun ifValue(i: Int, c: Boolean): Int = when (i) {
                    0 -> if (c) { println("c"); 1 } else 2
                    1 -> if (c) return 5 else 6
                    else -> 3
                }
                fun elvisValue(o: Any, s: String?): Boolean = when (o) { is Int -> s ?: return false; else -> "y" } == "x"
                fun tryValue(i: Int): Int = when (i) { 0 -> try { i / 0 } catch (e: Exception) { 1 }; else -> 2 }
                fun tryOfWhen(i: Int): String = try { when (i) { 0 -> throw IllegalStateException(); else -> "ok" } } catch (e: IllegalStateException) { "state" }
                fun assignedTryOfWhen(i: Int): Int { val v = try { when (i) { 0 -> 1; else -> 2 } } catch (e: Exception) { 3 }; return v }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.flatMap { it.recursiveSubTypeStream().toList() }
        .first { it.simpleName() == "K" }.methods().first { it.name() == name }.methodBody().statements()
        .joinToString(" ")

    @Test
    fun theShapes() {
        assertEquals("""
            single: return switch(i){case 0->"zero";default->"other";};
            block: return switch(i){case 0->{ConsoleKt.println("zero");yield 10;}default->{int j=i*2;yield j+1;}};
            jump: return switch(i){case 0->throw new IllegalStateException();default->i;};
            statement: switch(i){case 0->{ConsoleKt.println("zero");}default->{ConsoleKt.println("other");}}
            ifValue: return switch(i){case 0->{if(c){ConsoleKt.println("c");yield 1;}else{yield 2;}}case 1->{if(c){return 5;}else{yield 6;}}default->3;};
            elvisValue: return (switch(o){case int it->{if(s==null){return false;}yield s;}default->"y";}).equals("x");
            """.trimIndent(), listOf("single", "block", "jump", "statement", "ifValue", "elvisValue").joinToString("\n") { "$it: ${body(it)}" })
    }

    /* A `when` at the tail of a try used as a value IS the value. It kept its statement form, `"ok"` became an
       expression statement and the value was lost (jfocus-transform's Kotlin differential check, 2026-09-27). */
    @Test
    fun aWhenAtTheTailOfAValueTryIsAValue() {
        assertEquals("try{return switch(i){case 0->throw new IllegalStateException();default->\"ok\";};}"
                     + "catch(IllegalStateException e){return \"state\";}", body("tryOfWhen"))
        assertEquals(true, body("assignedTryOfWhen").contains("v=(switch(i){case 0->1;default->2;});"),
            body("assignedTryOfWhen"))
    }

    @Test
    fun aTryValueAssignsATemporary() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        val tryValue = body("tryValue")
        // the try assigns the temporary in each branch, and the arm yields it
        assertEquals(true, tryValue.contains("yield \$whenValue"), tryValue)
        assertEquals(true, tryValue.contains("catch(Exception e){\$whenValue"), tryValue)
    }
}
