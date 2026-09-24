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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * What the front end could not read has to be COUNTABLE, not merely present in the tree: downstream a `k2-…`
 * placeholder is EMPTY, so a body with a hole in it and a body with nothing to say produce the same verdicts.
 *
 * ⭐ The control ([aCleanParseReportsZeroAgainstARealDenominator]) is the test that keeps the others honest: a
 * census that walked nothing also reports 0, so the denominators are asserted, not just the total.
 */
class PlaceholderCensusTest : KotlinScanTestBase() {

    @Test
    fun anUnreadConstructIsCounted() {
        // ⚠ a REIFIED `T::class` rather than `String::class` or `s.length`: both stopped being holes (class literals,
        // §7.36; `bootstrapString` loading String's properties). A census test needs a construct actually missing.
        val types = KotlinScan(runtime, sourceSet).parse("u/U.kt", """
            package u
            inline fun <reified T> use(): Int = T::class.hashCode() + 1
            """.trimIndent() + "\n")
        val census = PlaceholderCensus.of(types)
        assertEquals(1, census.total, census.byKind.toString())
        assertEquals(1, census.types)
        assertEquals(1, census.members)
        assertTrue(census.typesVisited >= 1, "nothing walked: ${census.typesVisited}")
        assertTrue(census.membersVisited >= 1, "no members walked: ${census.membersVisited}")

        val report = census.report()
        assertTrue(report.contains("Kotlin placeholders: 1 in 1 of"), report)
        assertTrue(report.contains(census.byKind.keys.single()), report)
    }

    @Test
    fun aCleanParseReportsZeroAgainstARealDenominator() {
        val types = KotlinScan(runtime, sourceSet).parse("v/V.kt", """
            package v
            class Counter(private var count: Int) {
                fun add(delta: Int) { count = count + delta }
                fun value(): Int = count
            }
            """.trimIndent() + "\n")
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.byKind.toString())
        assertEquals(0, census.types)
        // ⭐ the walk really did visit the class and its members; "none" is a reading, not a silence
        assertTrue(census.typesVisited >= 1, "nothing walked: ${census.typesVisited}")
        assertTrue(census.membersVisited >= 3, "expected ctor + two methods + field, got ${census.membersVisited}")
        assertTrue(census.report().startsWith("Kotlin placeholders: none, in"), census.report())
    }

    /**
     * A placeholder built INSIDE a larger node (a selector, an indexed set, a synthesised delegate accessor) did not
     * pass through `convertExpression`'s range step and printed `0:0`: ~90 sites across detekt and coil that the
     * dump could not point at. The kinds are asserted too, so a census that found nothing cannot pass.
     */
    @Test
    fun everyPlaceholderCarriesAPosition() {
        val types = KotlinScan(runtime, sourceSet).parse("w/W.kt", """
            package w
            import kotlin.reflect.KProperty
            class Q
            operator fun Q.set(i: Int, v: Int) {}
            class D
            operator fun D.getValue(thisRef: Any?, property: KProperty<*>): Int = 1
            class U {
                val x: Int by D()
                fun f(s: String): Int = s.nope
                fun g(q: Q) { q[0] = 1 }
            }
            """.trimIndent() + "\n")
        val lines = PlaceholderCensus.of(types).dumpLines()
        val kinds = lines.map { it.substringBefore('\t') }.toSet()
        assertTrue(kinds.containsAll(setOf("k2-unresolved-access:nope", "k2-indexed-set-unresolved",
            "k2-delegate-read:x\$delegate")), lines.joinToString("\n"))
        assertTrue(lines.none { it.endsWith("\t0:0") }, lines.joinToString("\n"))
    }

    @Test
    fun anEmptyParseSaysSoRatherThanReportingClean() {
        val census = PlaceholderCensus.of(listOf())
        assertEquals(0, census.typesVisited)
        assertTrue(census.report().contains("nothing walked"), census.report())
    }
}
