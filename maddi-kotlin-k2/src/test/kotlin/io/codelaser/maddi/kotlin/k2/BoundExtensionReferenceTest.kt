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
 * A BOUND extension reference -- `::printRule` inside an extension on the receiver type, `n::printRule`, `::printRule`
 * in `with(n) { }` -- as the lambda `r -> Facade.printRule(receiver, r)`: no Java method reference binds a static
 * method's first argument. detekt's `rules.forEach(::printRule)` in `YamlNode.printRuleSet`.
 */
class BoundExtensionReferenceTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("br/Br.kt", """
            package br
            class Node { fun add(s: String) {} }
            fun Node.printRule(r: String) { add(r) }
            fun Node.all(rs: List<String>) { rs.forEach(::printRule) }
            fun explicit(n: Node, rs: List<String>) { rs.forEach(n::printRule) }
            fun viaWith(n: Node, rs: List<String>) { with(n) { rs.forEach(::printRule) } }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(name: String): String =
        type("BrKt").methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            all: CollectionsKt___CollectionsKt.forEach(rs,r->BrKt.printRule(${'$'}receiver,r));
            explicit: CollectionsKt___CollectionsKt.forEach(rs,r->BrKt.printRule(n,r));
            viaWith: StandardKt__StandardKt.with(n,${'$'}receiver->CollectionsKt___CollectionsKt.forEach(rs,r->BrKt.printRule(${'$'}receiver,r)));
            """.trimIndent(), listOf("all", "explicit", "viaWith").joinToString("\n") { "$it: ${body(it)}" })
    }
}
