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
