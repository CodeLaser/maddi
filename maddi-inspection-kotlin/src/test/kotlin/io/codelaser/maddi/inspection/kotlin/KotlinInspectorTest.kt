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

import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.net.URI

class KotlinInspectorTest {

    private val runtime: Runtime = RuntimeImpl()

    @Test
    fun parsesAndSharesRegistryWithCompiledTypesManager() {
        val sourceSet: SourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()

        val inspector = KotlinInspector(runtime)
        inspector.initialize(config)
        val types = inspector.parse(
            sourceSet,
            mapOf("Foo.kt" to "import java.util.UUID\nclass Foo { fun u(): UUID = UUID.randomUUID() }\n")
        )

        val foo = types.first()
        assertEquals("Foo", foo.simpleName())
        val uuidFromMethod = foo.findUniqueMethod("u", 0).returnType().typeInfo()

        // the CompiledTypesManager is a view over the same InfoByFqn: it returns the SAME instance
        // (identity invariant), with the hierarchy the scan loaded.
        val uuidFromManager = inspector.compiledTypesManager().typeIfLoaded("java.util.UUID", sourceSet)
        assertSame(uuidFromMethod, uuidFromManager)
        assertEquals("java.util.UUID", uuidFromManager!!.fullyQualifiedName())
    }
}
