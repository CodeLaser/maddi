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

import org.e2immu.language.cst.api.element.SourceSet
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
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
        val uuidFromManager = inspector.compiledTypesManager().get("java.util.UUID", sourceSet)
        assertSame(uuidFromMethod, uuidFromManager)
        assertEquals("java.util.UUID", uuidFromManager!!.fullyQualifiedName())
    }
}
