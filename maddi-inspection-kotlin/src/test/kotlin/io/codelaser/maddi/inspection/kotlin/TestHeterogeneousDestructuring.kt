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

import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl
import io.codelaser.maddi.inspection.resource.SourceSetImpl
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
