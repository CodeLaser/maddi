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

import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A local variable is not an Info, so it is not a reference target; its uses are recorded apart
 * ([io.codelaser.maddi.cst.api.element.DetailedSources.localReferences]), on the member whose text holds them, and
 * its declared name as a detail of that member keyed by the variable. Every kind of local: `val`/`var`, a loop
 * variable, a destructured one, a catch parameter and a `when` subject -- and a use inside a lambda passed to a
 * LIBRARY function, which the desugared CST may not convert at all (maddi#43) but K2 resolves.
 */
class LocalReferenceTest : KotlinScanTestBase() {

    private val source = """
        package r

        fun f(xs: List<Int>): Int {
            val a = 1
            var b = a + 1
            b += a
            for (x in xs) { b += x }
            val (p, q) = Pair(a, b)
            try { error("x") } catch (e: Exception) { println(e) }
            val s = "${'$'}a and ${'$'}{b}"
            return xs.map { it + a }.sum() + p + q + when (val w = b) { 1 -> w; else -> 0 } + s.length
        }
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun parse() {
        types = KotlinScan(runtime, sourceSet).parse("r/R.kt", source)
    }

    private fun f(): Info = types.first { it.simpleName() == "RKt" }.methods().first { it.name() == "f" }

    /** Every recorded use in [host], by the variable's name: distinct `line:column`s (a local may have two instances). */
    private fun uses(host: Info): Map<String, List<String>> {
        val byName = sortedMapOf<String, MutableSet<String>>()
        host.source().detailedSources()?.forEachLocalReference { variable, s ->
            byName.getOrPut(variable.simpleName()) { sortedSetOf() } += "${s.beginLine()}:${s.beginPos()}"
        }
        return byName.mapValues { it.value.toList() }
    }

    /** Every declared name in [host], by the variable's name. */
    private fun declarations(host: Info): Map<String, List<String>> {
        val byName = sortedMapOf<String, MutableSet<String>>()
        val ds = host.source().detailedSources() ?: return emptyMap()
        ds.forEachLocalReference { variable, _ ->
            ds.detail(variable)?.let {
                byName.getOrPut(variable.simpleName()) { sortedSetOf() } += "${it.beginLine()}:${it.beginPos()}"
            }
        }
        return byName.mapValues { it.value.toList() }
    }

    @Test
    fun everyUseOfEveryKindOfLocalIsRecordedOnItsMember() {
        parse()
        assertEquals(mapOf(
            "a" to listOf("10:15", "11:26", "5:13", "6:10", "8:23"),  // a template, a library lambda, ...
            "b" to listOf("10:23", "11:60", "6:5", "7:21", "8:26"),   // `b += a` is a use too
            "e" to listOf("9:55"),
            "p" to listOf("11:38"),
            "q" to listOf("11:42"),
            "s" to listOf("11:87"),
            "w" to listOf("11:70"),
            "x" to listOf("7:26"),
        ), uses(f()))
    }

    @Test
    fun everyLocalsDeclaredNameIsADetailOfItsMember() {
        parse()
        assertEquals(mapOf(
            "a" to listOf("4:9"), "b" to listOf("5:9"), "e" to listOf("9:31"), "p" to listOf("8:10"),
            "q" to listOf("8:13"), "s" to listOf("10:9"), "w" to listOf("11:56"), "x" to listOf("7:10"),
        ), declarations(f()))
    }

    /** Not a reference target: the dependency graph reads references(Info) as edges, and a local is no declaration. */
    @Test
    fun aLocalIsNotAReferenceTarget() {
        parse()
        val targets = mutableListOf<String>()
        f().source().detailedSources()?.forEachReference { target, _ -> targets += target.simpleName() }
        assertTrue(targets.none { it in setOf("a", "b", "e", "p", "q", "s", "w", "x") }, targets.toString())
    }
}
