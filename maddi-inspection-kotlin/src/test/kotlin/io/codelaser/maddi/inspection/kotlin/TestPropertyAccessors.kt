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

package org.e2immu.language.inspection.kotlin

import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * A `const val` (inlined static final) and a `private` property have no getter method on the JVM, so the
 * front-end must not synthesise one — a synthetic getter clashes with an explicitly-declared getX() of the
 * same signature (e.g. kotlin-stdlib's `DoubleCompanionObject.getMIN_VALUE` next to `const val MIN_VALUE`, and
 * `ParameterizedTypeImpl.getOwnerType()` overriding a Java interface next to `private val ownerType`).
 */
class TestPropertyAccessors {

    private fun parse(src: String): List<TypeInfo> {
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()
        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)
        return inspector.parse(sourceSet, mapOf("A.kt" to src))
    }

    @Test
    fun constValGetsNoSyntheticGetter() {
        val types = parse(
            """
            package p
            object O {
                const val MIN: Int = 5
                fun getMIN(): Int = MIN
            }
            """.trimIndent()
        )
        val o = types.first { it.simpleName() == "O" }
        assertEquals(1, o.methods().count { it.name() == "getMIN" }, "const val must not add a second getMIN")
    }

    @Test
    fun privateValGetsNoSyntheticGetter() {
        val types = parse(
            """
            package p
            class X(private val ownerType: Int) {
                fun getOwnerType(): Int = ownerType
            }
            """.trimIndent()
        )
        val x = types.first { it.simpleName() == "X" }
        assertEquals(1, x.methods().count { it.name() == "getOwnerType" },
            "a private val must not add a synthetic getter clashing with the explicit one")
    }
}
