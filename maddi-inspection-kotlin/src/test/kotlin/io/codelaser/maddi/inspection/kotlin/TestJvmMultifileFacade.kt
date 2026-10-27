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

import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
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
