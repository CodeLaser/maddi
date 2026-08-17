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

import org.e2immu.language.cst.api.statement.LocalVariableCreation
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.InputConfigurationImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

/**
 * A Kotlin destructuring `val (a, b) = pair` binds independently-typed components (`component1()`,
 * `component2()`) to one `LocalVariableCreation`; unlike Java's `int a, b`, the variables need NOT share a base
 * type — so the CST-core same-base-type invariant must not reject them.
 */
class TestHeterogeneousDestructuring {

    @Test
    fun differentlyTypedComponents() {
        val sourceSet = SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/")).build()
        val config = InputConfigurationImpl.Builder().addSourceSets(sourceSet).build()
        val inspector = KotlinInspector(RuntimeImpl())
        inspector.initialize(config)

        val types = inspector.parse(
            sourceSet, mapOf(
                "A.kt" to """
                    package p
                    data class Person(val name: String, val age: Int)
                    fun f(person: Person): Int {
                        val (name, age) = person
                        return name.length + age
                    }
                """.trimIndent()
            )
        )

        val facade = types.first { it.simpleName() == "AKt" }
        val f = facade.findUniqueMethod("f", 1)
        val lvc = f.methodBody().statements().filterIsInstance<LocalVariableCreation>().first()
        // two variables of different types on one creation: name:String, age:Int
        assertEquals("name", lvc.localVariable().simpleName())
        assertEquals(1, lvc.otherLocalVariables().size)
        assertEquals("age", lvc.otherLocalVariables().first().simpleName())
        assertEquals("String", lvc.localVariable().parameterizedType().fullyQualifiedName())
        assertEquals("int", lvc.otherLocalVariables().first().parameterizedType().fullyQualifiedName())
    }
}
