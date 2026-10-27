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

package io.codelaser.maddi.inspection.kotlin

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
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
