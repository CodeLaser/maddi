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
        // ⚠ `String::class` rather than `s.length`: the latter stopped being a hole when `bootstrapString`
        // learned to load String's properties. A census test needs a construct that is actually missing.
        val types = KotlinScan(runtime, sourceSet).parse("u/U.kt", """
            package u
            fun use(s: String) = String::class.hashCode() + 1
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

    @Test
    fun anEmptyParseSaysSoRatherThanReportingClean() {
        val census = PlaceholderCensus.of(listOf())
        assertEquals(0, census.typesVisited)
        assertTrue(census.report().contains("nothing walked"), census.report())
    }
}
