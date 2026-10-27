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
 * #54: `class Del(d: I) : I by d` produced forwarders with EMPTY bodies -- "modifies nothing" downstream, whatever
 * the delegate does -- and no accessor for an interface property. As kotlinc compiles it, the delegate is kept in
 * `$$delegate_0`, assigned from the `by` expression, and every member forwards to it.
 */
class ClassDelegationTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("cd/Cd.kt", """
            package cd
            interface I { fun f(x: String): Int; fun g(); val p: Int; var q: String }
            class Del(d: I) : I by d
            class Kept(private val d: I) : I by d { override fun g() { } }
            """.trimIndent() + "\n")
    }

    private fun shape(name: String): String {
        val t = types.first { it.simpleName() == name }
        val fields = t.fields().joinToString(" ") { "${it.name()}=${it.initializer()}" }
        val methods = t.methods().sortedBy { it.name() }
            .joinToString(" ") { "${it.name()}${it.parameters().map { p -> p.name() }}:${it.methodBody().statements()}" }
        return "$fields | $methods"
    }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theDelegateIsKeptAndEveryMemberForwards() {
        // Kept's own `override fun g() { }` is not a forwarder: its (empty) body is the source's
        assertEquals("""
            Del: ${'$'}${'$'}delegate_0=<empty> | f[x]:[return this.${'$'}${'$'}delegate_0.f(x);] g[]:[this.${'$'}${'$'}delegate_0.g();] getP[]:[return this.${'$'}${'$'}delegate_0.getP();] getQ[]:[return this.${'$'}${'$'}delegate_0.getQ();] setQ[value]:[this.${'$'}${'$'}delegate_0.setQ(value);]
            Kept: d=<empty> ${'$'}${'$'}delegate_0=<empty> | f[x]:[return this.${'$'}${'$'}delegate_0.f(x);] g[]:[] getP[]:[return this.${'$'}${'$'}delegate_0.getP();] getQ[]:[return this.${'$'}${'$'}delegate_0.getQ();] setQ[value]:[this.${'$'}${'$'}delegate_0.setQ(value);]
            """.trimIndent(), listOf("Del", "Kept").joinToString("\n") { "$it: ${shape(it)}" })
        // the `by` expression reads the constructor parameter, so the delegate is assigned IN the constructor, as
        // kotlinc does (#85); the field keeps no initializer of its own
        assertEquals("this.${'$'}${'$'}delegate_0=d;",
            types.first { it.simpleName() == "Del" }.findConstructor(1).methodBody().statements().joinToString(" "))
        assertEquals("this.d=d; this.${'$'}${'$'}delegate_0=d;",
            types.first { it.simpleName() == "Kept" }.findConstructor(1).methodBody().statements().joinToString(" "))
    }
}
