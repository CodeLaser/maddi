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

import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * Kotlin-multiplatform `expect`/`actual`: an `expect` declaration (commonMain) and its `actual` realisation
 * (jvmMain) share one FQN. On the JVM the `actual` is authoritative; the front-end must drop the `expect` so
 * the two don't collide as "Duplicating type" (e.g. `kotlin.Annotation` is expect in common, actual in jvm).
 */
class TestExpectActual {

    @Test
    fun expectIsDroppedActualIsKept() {
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()
        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)

        val types = inspector.parse(
            sourceSet, mapOf(
                "Common.kt" to "package p\nexpect class Foo {\n    fun bar(): Int\n}\n",
                "Jvm.kt" to "package p\nactual class Foo {\n    actual fun bar(): Int = 1\n}\n",
            )
        )

        val foos = types.filter { it.fullyQualifiedName() == "p.Foo" }
        assertEquals(1, foos.size, "the expect/actual pair must yield a single type")
        // the surviving type is the actual: its method has a body
        assertEquals(setOf("bar"), foos.first().methods().map { it.name() }.toSet())
    }
}
