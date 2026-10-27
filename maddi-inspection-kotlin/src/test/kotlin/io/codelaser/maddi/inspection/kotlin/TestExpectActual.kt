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
