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

package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * An override returning a primitive, of a member whose own return type is NOT primitive (a type parameter, a
 * nullable, `Any`): kotlinc returns it BOXED, with a bridge for the erased signature. Measured with javap on Exposed:
 * `ShortColumnType : ColumnType<Short>` has `java.lang.Short valueFromDB(Object)`; `InsertBlockingExecutable` has
 * `java.lang.Integer executeInternal(...)`. A primitive one does not override `T valueFromDB(Object)` -- the mixed
 * parse's Java stubs did not compile -- and the CST says a value is unboxed where the JVM boxes it.
 */
class OverrideBoxedReturnTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ob/Ob.kt", """
            package ob
            interface Col<T> { fun fromDb(v: Any): T? }
            class ShortCol : Col<Short> { override fun fromDb(v: Any): Short = 1 }
            abstract class Exec<T> { abstract fun run(): T }
            class IntExec : Exec<Int>() { override fun run(): Int = 1 }
            interface Counted { fun count(): Int }
            class CountedImpl : Counted { override fun count(): Int = 1 }
            open class Base { open fun any(): Any = 0 }
            class Narrowed : Base() { override fun any(): Long = 1L }
            class Plain { fun f(): Int = 1 }
            """.trimIndent() + "\n")
    }

    private fun returnOf(type: String, method: String): String =
        types.first { it.simpleName() == type }.methods().single { it.name() == method }.returnType().detailedString()

    @Test
    fun boxedWhereTheOverriddenReturnIsNotPrimitive() {
        assertEquals("Short", returnOf("ShortCol", "fromDb"))
        assertEquals("Integer", returnOf("IntExec", "run"))
        assertEquals("Long", returnOf("Narrowed", "any"))
    }

    @Test
    fun primitiveWhereItOverridesAPrimitiveOrNothing() {
        assertEquals("int", returnOf("CountedImpl", "count"))
        assertEquals("int", returnOf("Plain", "f"))
    }
}
