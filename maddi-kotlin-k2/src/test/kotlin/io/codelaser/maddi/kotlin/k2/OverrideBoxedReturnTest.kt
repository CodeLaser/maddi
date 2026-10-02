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
