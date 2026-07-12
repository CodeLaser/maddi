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
 * `@JvmMultifileClass`: several source files contribute their top-level functions/properties to ONE JVM facade
 * class (e.g. `kotlin.collections.CollectionsKt` spans ten files in kotlin-stdlib). The front-end must produce a
 * single merged facade `TypeInfo` rather than one per file (which collided as "Duplicating type").
 */
class TestJvmMultifileFacade {

    @Test
    fun twoFilesShareOneFacade() {
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()
        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)

        val types = inspector.parse(
            sourceSet, mapOf(
                "A.kt" to "@file:kotlin.jvm.JvmMultifileClass\n@file:kotlin.jvm.JvmName(\"UtilKt\")\npackage p\nfun a(): Int = 1\n",
                "B.kt" to "@file:kotlin.jvm.JvmMultifileClass\n@file:kotlin.jvm.JvmName(\"UtilKt\")\npackage p\nfun b(): Int = 2\n",
            )
        )

        val facades = types.filter { it.simpleName() == "UtilKt" }
        assertEquals(1, facades.size, "the two @JvmMultifileClass files must share ONE facade")
        val facade = facades.first()
        assertEquals("p.UtilKt", facade.fullyQualifiedName())
        // both files' top-level functions land on the single merged facade
        assertEquals(setOf("a", "b"), facade.methods().map { it.name() }.toSet())
    }
}
