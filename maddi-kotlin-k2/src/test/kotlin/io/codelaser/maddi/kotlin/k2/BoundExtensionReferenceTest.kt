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
            all: CollectionsKt.forEach(rs,r->BrKt.printRule(${'$'}receiver,r));
            explicit: CollectionsKt.forEach(rs,r->BrKt.printRule(n,r));
            viaWith: {CollectionsKt.forEach(rs,r->BrKt.printRule(n,r));}
            """.trimIndent(), listOf("all", "explicit", "viaWith").joinToString("\n") { "$it: ${body(it)}" })
    }
}
