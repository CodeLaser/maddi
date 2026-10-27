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

import io.codelaser.maddi.kotlin.k2.ReferenceRecall.Region
import io.codelaser.maddi.kotlin.k2.ReferenceRecall.Site
import io.codelaser.maddi.kotlin.k2.ReferenceRecall.Target
import io.codelaser.maddi.kotlin.k2.ReferenceRecall.Tier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The instrument, graded against references whose fate in the CST is known. Each case pins today's CST as much as
 * the instrument: when the Kotlin front-end starts converting a dropped region, the DROPPED rows here are expected
 * to move up, and the test should be updated to say so.
 */
class ReferenceRecallTest : KotlinScanTestBase() {

    private val model = """
        package a

        class Model(val size: Int) {
            val doubled = size * 2
            fun grow(by: Int): Int {
                val next = size + by
                return next
            }
            fun all(items: List<Model>): List<Int> = items.map { it.grow(1) }
            companion object {
                fun create(): Model = Model(0)
            }
        }
        """.trimIndent() + "\n"

    private val user = """
        package b

        import a.Model

        class User {
            fun use(m: Model): Int = m.grow(3)
            fun make(): Model = Model.create()
        }
        """.trimIndent() + "\n"

    private fun rows(): Map<String, ReferenceRecall.Row> {
        val recall = ReferenceRecall()
        KotlinScan(runtime, sourceSet).parse(mapOf("a/Model.kt" to model, "b/User.kt" to user), emptyMap(), listOf(recall))
        assertEquals(0, recall.unresolvedReferences, "every project reference must resolve")
        assertEquals(0, recall.failedReferences)
        return recall.rows().associateBy { "${it.file.substringAfterLast('/')}:${it.line}:${it.column}" }
    }

    private fun assertRow(rows: Map<String, ReferenceRecall.Row>, at: String, name: String, site: Site,
                          region: Region, target: Target, tier: Tier) {
        val row = rows[at] ?: throw AssertionError("no reference measured at $at; measured: ${rows.keys}")
        assertEquals(listOf(name, site, region, target, tier),
            listOf(row.name, row.site, row.region, row.target, row.tier), "reference at $at")
    }

    @Test
    fun convertedReferencesAreExact() {
        val rows = rows()
        assertRow(rows, "Model.kt:6:20", "size", Site.NAME, Region.FUNCTION_BODY, Target.PROPERTY, Tier.EXACT)
        assertRow(rows, "Model.kt:6:27", "by", Site.NAME, Region.FUNCTION_BODY, Target.PARAMETER, Tier.EXACT)
        assertRow(rows, "Model.kt:7:16", "next", Site.NAME, Region.FUNCTION_BODY, Target.LOCAL, Tier.EXACT)
        // a type argument in a signature: the nested type reference carries its own detail
        assertRow(rows, "Model.kt:9:25", "Model", Site.TYPE_REFERENCE, Region.DECLARATION_HEADER, Target.TYPE, Tier.EXACT)
        assertRow(rows, "Model.kt:11:31", "Model", Site.CALLEE, Region.FUNCTION_BODY, Target.CONSTRUCTOR, Tier.EXACT)
        assertRow(rows, "User.kt:6:16", "Model", Site.TYPE_REFERENCE, Region.DECLARATION_HEADER, Target.TYPE, Tier.EXACT)
        assertRow(rows, "User.kt:6:32", "grow", Site.CALLEE, Region.FUNCTION_BODY, Target.FUNCTION, Tier.EXACT)
    }

    @Test
    fun desugaredReceiverIsCoveredNotReferenced() {
        // `Model.create()` becomes `Model.Companion.create()`: the call is converted, the `Model` qualifier is not
        assertRow(rows(), "User.kt:7:25", "Model", Site.NAME, Region.FUNCTION_BODY, Target.TYPE, Tier.COVERED)
    }

    @Test
    fun aPropertyInitializerIsConverted() {
        // it was not, and every reference in one was DROPPED; it is now its field's initializer
        assertRow(rows(), "Model.kt:4:19", "size", Site.NAME, Region.PROPERTY_INITIALIZER, Target.PROPERTY, Tier.EXACT)
    }

    /**
     * `items.map { it.grow(1) }`. It used to be one placeholder -- an unresolved library extension call takes its
     * receiver and its lambda with it -- and both rows were DROPPED. maddi#43 converts the call to the facade
     * static `CollectionsKt.map(items, lambda)`, so the receiver and everything the lambda declares survive.
     */
    @Test
    fun aCallToALibraryExtensionIsConverted() {
        val rows = rows()
        assertRow(rows, "Model.kt:9:46", "items", Site.NAME, Region.FUNCTION_BODY, Target.PARAMETER, Tier.EXACT)
        assertRow(rows, "Model.kt:9:61", "grow", Site.CALLEE, Region.LAMBDA_TO_LIBRARY, Target.FUNCTION, Tier.EXACT)
    }

    @Test
    fun unconvertedRegionsAreDropped() {
        // import directives are not recorded
        assertRow(rows(), "User.kt:3:10", "Model", Site.IMPORT, Region.IMPORT, Target.TYPE, Tier.DROPPED)
    }
}
