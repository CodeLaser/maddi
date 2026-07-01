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

package org.e2immu.language.inspection.kotlin.prepwork

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `callgraph/TestComputePartOfConstruction` test3/test4: a method
 * (`update()`) called from the constructor AND from a lambda / anonymous-class body inside another method is
 * NOT part-of-construction (it is reachable outside construction). This directly probes whether the Kotlin
 * front-end represents the call made inside a lambda / anonymous body — if it does not, `update()` would
 * appear constructor-only and wrongly land in PART_OF_CONSTRUCTION.
 */
class TestComputePartOfConstruction : CommonKotlinPrep() {

    private fun partOfConstruction(source: String): String {
        val x = parse("X.kt", source)
        PrepAnalyzer(runtime).doPrimaryType(x)
        return x.analysis().getOrDefault(PART_OF_CONSTRUCTION, EMPTY_PART_OF_CONSTRUCTION).infoSet().toString()
    }

    @Test
    fun test3_lambda() {
        assertEquals(
            "[a.b.X.<init>()]",
            partOfConstruction(
                """
                package a.b
                class X {
                    private var i: Int = 0
                    constructor() { update() }
                    fun init() { val r = { update() }; r() }
                    private fun update() { ++i }
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun test4_anonymous() {
        assertEquals(
            "[a.b.X.<init>()]",
            partOfConstruction(
                """
                package a.b
                class X {
                    private var i: Int = 0
                    constructor() { update() }
                    fun init() {
                        val r = object : Runnable { override fun run() { update() } }
                        r.run()
                    }
                    private fun update() { ++i }
                }
                """.trimIndent()
            )
        )
    }
}
